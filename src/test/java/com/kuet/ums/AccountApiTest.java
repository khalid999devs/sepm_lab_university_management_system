package com.kuet.ums;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.kuet.ums.dto.CreateAccountRequest;
import com.kuet.ums.entity.*;
import com.kuet.ums.exception.ApiException;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.security.AccountPrincipal;
import com.kuet.ums.service.AccountService;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties = "spring.config.import=")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class AccountApiTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    AccountRepository accounts;

    @Autowired
    AccountService service;

    @Autowired
    PasswordEncoder passwords;

    private final Map<Role, UserAccount> users = new EnumMap<>(Role.class);

    @BeforeEach
    void resetAccounts() {
        accounts.deleteAll();
        String hash = passwords.encode("TestPass123");
        for (Role role : Role.values()) {
            users.put(
                role,
                accounts.saveAndFlush(
                    new UserAccount(
                        "Test " + role,
                        role.name().toLowerCase(Locale.ROOT) + "@ums.test",
                        hash,
                        role
                    )
                )
            );
        }
    }

    static Stream<Arguments> permissionPairs() {
        return Arrays.stream(Role.values()).flatMap((actor) ->
            Arrays.stream(Role.values()).map((resource) -> Arguments.of(actor, resource))
        );
    }

    @ParameterizedTest(name = "{0} manages {1}")
    @MethodSource("permissionPairs")
    void enforcesEveryRoleResourcePairForEveryCrudOperation(Role actor, Role resource)
        throws Exception {
        String route = route(resource);
        long id = users.get(resource).getId();
        AccountPrincipal principal = new AccountPrincipal(users.get(actor));
        boolean allowed = actor == Role.ADMIN || actor == resource;
        boolean admin = actor == Role.ADMIN;
        int expected = allowed ? 200 : 403;
        mvc.perform(get(route).with(user(principal))).andExpect(status().is(expected));
        mvc.perform(get(route + "/" + id).with(user(principal))).andExpect(status().is(expected));
        MvcResult created = mvc
            .perform(
                post(route)
                    .with(user(principal))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body("New Account", "new@ums.test", "NewPass123"))
            )
            .andExpect(status().is(admin ? 201 : 403))
            .andReturn();
        mvc.perform(
            put(route + "/" + id)
                .with(user(principal))
                .with(csrf())
                .contentType("application/json")
                .content(body("Updated Account", users.get(resource).getEmail(), null))
        ).andExpect(status().is(expected));
        long deleteId = admin
            ? json.readTree(created.getResponse().getContentAsString()).get("id").asLong()
            : id;
        mvc.perform(
            delete(route + "/" + deleteId)
                .with(user(principal))
                .with(csrf())
        ).andExpect(status().is(admin ? 204 : 403));
        if (!allowed) {
            assertThat(accounts.count()).isEqualTo(3);
            assertThat(accounts.findById(id).orElseThrow().getFullName()).isEqualTo(
                "Test " + resource
            );
        }
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void crudUsesLocationJsonAndAnEmptyDeleteResponse(Role role) throws Exception {
        AccountPrincipal admin = new AccountPrincipal(users.get(Role.ADMIN));
        String route = route(role);
        MvcResult result = mvc
            .perform(
                post(route)
                    .with(user(admin))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body("  New Person  ", "  PERSON@UMS.TEST  ", "NewPass123"))
            )
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.fullName").value("New Person"))
            .andExpect(jsonPath("$.email").value("person@ums.test"))
            .andExpect(jsonPath("$.role").value(role.name()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(route + "/" + id);
        UserAccount stored = accounts.findById(id).orElseThrow();
        String hash = stored.getPasswordHash();
        assertThat(hash).isNotEqualTo("NewPass123");
        assertThat(passwords.matches("NewPass123", hash)).isTrue();
        mvc.perform(get(route + "/" + id).with(user(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(route).with(user(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].id").value(id));
        for (int repeat = 0; repeat < 2; repeat++) {
            mvc.perform(
                put(route + "/" + id)
                    .with(user(admin))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body("Edited Person", "edited@ums.test", null))
            ).andExpect(status().isOk());
        }

        assertThat(accounts.findById(id).orElseThrow().getPasswordHash()).isEqualTo(hash);
        mvc.perform(
            delete(route + "/" + id)
                .with(user(admin))
                .with(csrf())
        )
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
        mvc.perform(
            delete(route + "/" + id)
                .with(user(admin))
                .with(csrf())
        ).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void unauthenticatedClientsCannotUseAnyCrudOperation(Role role) throws Exception {
        String route = route(role);
        long id = users.get(role).getId();
        mvc.perform(get(route))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get(route + "/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(
            post(route)
                .with(csrf())
                .contentType("application/json")
                .content(body("New Person", "new@ums.test", "NewPass123"))
        ).andExpect(status().isUnauthorized());
        mvc.perform(
            put(route + "/" + id)
                .with(csrf())
                .contentType("application/json")
                .content(body("New Person", "new@ums.test", null))
        ).andExpect(status().isUnauthorized());
        mvc.perform(delete(route + "/" + id).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRefreshesCsrfAndLogoutInvalidatesSession() throws Exception {
        MvcResult tokenResult = mvc
            .perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        JsonNode before = json.readTree(tokenResult.getResponse().getContentAsString());
        mvc.perform(
            post("/api/auth/login")
                .session(session)
                .header(before.get("headerName").asText(), before.get("token").asText())
                .contentType("application/x-www-form-urlencoded")
                .param("email", " ADMIN@UMS.TEST ")
                .param("password", "TestPass123")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(users.get(Role.ADMIN).getId()));
        mvc.perform(
            post("/api/teachers")
                .session(session)
                .header(before.get("headerName").asText(), before.get("token").asText())
                .contentType("application/json")
                .content(body("New Person", "new@ums.test", "NewPass123"))
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_ERROR"));
        JsonNode after = token(session);
        mvc.perform(
            post("/api/auth/logout")
                .session(session)
                .header(after.get("headerName").asText(), after.get("token").asText())
        )
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsIncorrectCredentialsAndLoginLogoutRequireCsrf() throws Exception {
        mvc.perform(
            post("/api/auth/login")
                .param("email", "admin@ums.test")
                .param("password", "TestPass123")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("email", "admin@ums.test")
                .param("password", "incorrect")
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        MockHttpSession session = login(Role.ADMIN);
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/logout").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "DELETE" })
    void accountWritesRequireCsrf(String method) throws Exception {
        String route =
            "/api/teachers" + (method.equals("POST") ? "" : "/" + users.get(Role.TEACHER).getId());
        mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), route)
                .with(user(new AccountPrincipal(users.get(Role.ADMIN))))
                .contentType("application/json")
                .content(body("New Person", "new@ums.test", "NewPass123"))
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_ERROR"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{}",
            "{\"fullName\":\"A\",\"email\":\"wrong\",\"password\":\"short\"}",
            "{\"fullName\":\"   \",\"email\":\"name@ums.test\",\"password\":\"TestPass123\"}",
            "{\"fullName\":\"Person\",\"email\":\"name@ums.test\",\"password\":\"TestPass123\",\"role\":\"ADMIN\"}",
            "{\"fullName\":\"Person\",\"email\":\"name@ums.test\",\"password\":\"TestPass123\",\"id\":99}",
            "{\"fullName\":\"Person\",\"email\":\"name@ums.test\",\"passwordHash\":\"fake\"}",
            "{broken"
        }
    )
    void invalidAndServerManagedFieldsAreRejected(String body) throws Exception {
        mvc.perform(
            post("/api/teachers")
                .with(user(new AccountPrincipal(users.get(Role.ADMIN))))
                .with(csrf())
                .contentType("application/json")
                .content(body)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        assertThat(accounts.count()).isEqualTo(3);
    }

    @Test
    void validationEnforcesPasswordByteLimitAndCompleteUpdates() throws Exception {
        AccountPrincipal admin = new AccountPrincipal(users.get(Role.ADMIN));
        mvc.perform(
            post("/api/teachers")
                .with(user(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body("Person", "person@ums.test", "é".repeat(37)))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.password").exists());
        mvc.perform(
            post("/api/teachers")
                .with(user(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body("Person", "person@ums.test", "a".repeat(72)))
        ).andExpect(status().isCreated());
        mvc.perform(
            put("/api/teachers/" + users.get(Role.TEACHER).getId())
                .with(user(admin))
                .with(csrf())
                .contentType("application/json")
                .content("{\"fullName\":\"Person\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void duplicateEmailIsGlobalAndCaseInsensitive() throws Exception {
        AccountPrincipal admin = new AccountPrincipal(users.get(Role.ADMIN));
        mvc.perform(
            post("/api/students")
                .with(user(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body("Person", "ADMIN@UMS.TEST", "TestPass123"))
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
        mvc.perform(
            put("/api/teachers/" + users.get(Role.TEACHER).getId())
                .with(user(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body("Person", "student@ums.test", null))
        ).andExpect(status().isConflict());
    }

    @Test
    void unknownAndWrongCollectionIdsNeverCreateOrModifyAccounts() throws Exception {
        AccountPrincipal admin = new AccountPrincipal(users.get(Role.ADMIN));
        for (long id : new long[] { Long.MAX_VALUE, users.get(Role.STUDENT).getId() }) {
            mvc.perform(get("/api/teachers/" + id).with(user(admin))).andExpect(
                status().isNotFound()
            );
            mvc.perform(
                put("/api/teachers/" + id)
                    .with(user(admin))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body("Person", "person@ums.test", null))
            ).andExpect(status().isNotFound());
            mvc.perform(
                delete("/api/teachers/" + id)
                    .with(user(admin))
                    .with(csrf())
            ).andExpect(status().isNotFound());
        }

        assertThat(accounts.count()).isEqualTo(3);
        mvc.perform(get("/api/teachers/not-a-number").with(user(admin))).andExpect(
            status().isBadRequest()
        );
    }

    @Test
    void methodAndContentTypeErrorsHaveRestStatusCodes() throws Exception {
        AccountPrincipal admin = new AccountPrincipal(users.get(Role.ADMIN));
        mvc.perform(
            patch("/api/teachers/" + users.get(Role.TEACHER).getId())
                .with(user(admin))
                .with(csrf())
                .contentType("application/json")
                .content("{}")
        )
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().exists("Allow"))
            .andExpect(jsonPath("$.status").value(405));
        mvc.perform(
            post("/api/teachers")
                .with(user(admin))
                .with(csrf())
                .contentType("text/plain")
                .content("plain")
        )
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void deletingTheFinalAdminReturnsConflict() throws Exception {
        mvc.perform(
            delete("/api/admins/" + users.get(Role.ADMIN).getId())
                .with(user(new AccountPrincipal(users.get(Role.ADMIN))))
                .with(csrf())
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("FINAL_ADMIN"));
        assertThat(accounts.findAllByRoleOrderByIdAsc(Role.ADMIN)).hasSize(1);
    }

    @Test
    void concurrentAdminDeletionsStillLeaveOneAdmin() throws Exception {
        long first = users.get(Role.ADMIN).getId();
        long second = asRole(Role.ADMIN, () ->
            service.create(
                Role.ADMIN,
                new CreateAccountRequest("Second Admin", "second@ums.test", "TestPass123")
            )
        ).id();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (long id : new long[] { first, second }) {
                results.add(
                    threads.submit(() -> {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Concurrent test did not start.");
                        }

                        try {
                            return asRole(Role.ADMIN, () -> {
                                service.delete(Role.ADMIN, id);

                                return 204;
                            });
                        } catch (ApiException ex) {
                            return ex.getStatus().value();
                        }
                    })
                );
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(
                List.of(
                    results.get(0).get(10, TimeUnit.SECONDS),
                    results.get(1).get(10, TimeUnit.SECONDS)
                )
            ).containsExactlyInAnyOrder(204, 409);
            assertThat(accounts.findAllByRoleOrderByIdAsc(Role.ADMIN)).hasSize(1);
        } finally {
            start.countDown();
            threads.shutdownNow();
        }
    }

    @Test
    void deletedAccountsLoseAccessOnTheirNextRequest() throws Exception {
        MockHttpSession session = login(Role.TEACHER);
        mvc.perform(
            delete("/api/teachers/" + users.get(Role.TEACHER).getId())
                .with(user(new AccountPrincipal(users.get(Role.ADMIN))))
                .with(csrf())
        ).andExpect(status().isNoContent());
        mvc.perform(get("/api/teachers").session(session)).andExpect(status().isUnauthorized());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void editingOwnEmailKeepsSessionIdentityAndPasswordUpdatesAreHashed() throws Exception {
        MockHttpSession session = login(Role.TEACHER);
        mvc.perform(
            put("/api/teachers/" + users.get(Role.TEACHER).getId())
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(body("Changed Teacher", "changed@ums.test", "ChangedPass123"))
        ).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("changed@ums.test"));
        assertThat(
            passwords.matches(
                "ChangedPass123",
                accounts.findByEmail("changed@ums.test").orElseThrow().getPasswordHash()
            )
        ).isTrue();
        mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("email", "changed@ums.test")
                .param("password", "TestPass123")
        ).andExpect(status().isUnauthorized());
        mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("email", "changed@ums.test")
                .param("password", "ChangedPass123")
        ).andExpect(status().isOk());
    }

    @Test
    void emptyCollectionsReturnAnEmptyArrayAndReadsDoNotChangeData() throws Exception {
        accounts.delete(users.get(Role.TEACHER));
        mvc.perform(get("/api/teachers").with(user(new AccountPrincipal(users.get(Role.ADMIN)))))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
        assertThat(accounts.count()).isEqualTo(2);
    }

    @Test
    void pagesAndAssetsAreServedWithoutAnAuthenticatedSession() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login.html"));
        for (String path : List.of(
            "/login.html",
            "/management.html",
            "/css/styles.css",
            "/js/api.js",
            "/js/login.js",
            "/js/management.js",
            "/js/ui.js",
            "/favicon.svg"
        )) {
            mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "TEACHER", "STUDENT" })
    void personalAccountsCannotReadOrChangeOtherPeopleOfTheSameRole(Role role) throws Exception {
        UserAccount own = users.get(role);
        UserAccount other = accounts.saveAndFlush(
            new UserAccount(
                "Other Person",
                "other@ums.test",
                passwords.encode("OtherPass123"),
                role
            )
        );
        MockHttpSession session = login(role);
        String route = route(role);
        mvc.perform(get(route).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(own.getId()));
        mvc.perform(get(route + "/" + own.getId()).session(session)).andExpect(status().isOk());
        for (long id : new long[] { other.getId(), Long.MAX_VALUE }) {
            mvc.perform(get(route + "/" + id).session(session)).andExpect(status().isNotFound());
            mvc.perform(
                put(route + "/" + id)
                    .session(session)
                    .with(csrf())
                    .contentType("application/json")
                    .content(body("Changed Person", "changed@ums.test", "ChangedPass123"))
            ).andExpect(status().isNotFound());
            mvc.perform(
                delete(route + "/" + id)
                    .session(session)
                    .with(csrf())
            ).andExpect(status().isForbidden());
        }

        mvc.perform(
            delete(route + "/" + own.getId())
                .session(session)
                .with(csrf())
        ).andExpect(status().isForbidden());
        mvc.perform(
            post(route)
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(body("New Person", "new@ums.test", "NewPass123"))
        ).andExpect(status().isForbidden());
        mvc.perform(
            put(route + "/" + own.getId())
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(body("My Updated Name", own.getEmail(), null))
        ).andExpect(status().isOk());
        assertThat(accounts.findById(other.getId()).orElseThrow().getFullName()).isEqualTo(
            "Other Person"
        );
        mvc.perform(get(route).with(user(new AccountPrincipal(users.get(Role.ADMIN)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "TEACHER", "STUDENT" })
    void sharedServiceAlsoEnforcesOwnershipAndAdminOnlyWrites(Role role) throws Exception {
        UserAccount other = accounts.saveAndFlush(
            new UserAccount(
                "Other Person",
                "other@ums.test",
                passwords.encode("OtherPass123"),
                role
            )
        );
        asRole(role, () -> {
            assertThat(service.list(role))
                .extracting((account) -> account.id())
                .containsExactly(users.get(role).getId());
            assertThatThrownBy(() -> service.get(role, other.getId()))
                .isInstanceOf(ApiException.class)
                .extracting((ex) -> ((ApiException) ex).getStatus().value())
                .isEqualTo(404);
            assertThatThrownBy(() ->
                service.update(
                    role,
                    other.getId(),
                    new com.kuet.ums.dto.UpdateAccountRequest(
                        "Changed Person",
                        "changed@ums.test",
                        null
                    )
                )
            )
                .isInstanceOf(ApiException.class)
                .extracting((ex) -> ((ApiException) ex).getStatus().value())
                .isEqualTo(404);
            assertThatThrownBy(() ->
                service.create(
                    role,
                    new CreateAccountRequest("New Person", "new@ums.test", "TestPass123")
                )
            )
                .isInstanceOf(ApiException.class)
                .extracting((ex) -> ((ApiException) ex).getStatus().value())
                .isEqualTo(403);
            assertThatThrownBy(() -> service.delete(role, users.get(role).getId()))
                .isInstanceOf(ApiException.class)
                .extracting((ex) -> ((ApiException) ex).getStatus().value())
                .isEqualTo(403);
            Role foreign = role == Role.TEACHER ? Role.STUDENT : Role.TEACHER;
            assertThatThrownBy(() -> service.list(foreign))
                .isInstanceOf(ApiException.class)
                .extracting((ex) -> ((ApiException) ex).getStatus().value())
                .isEqualTo(403);

            return null;
        });
    }

    private <T> T asRole(Role role, Callable<T> action) throws Exception {
        SecurityContext original = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        AccountPrincipal principal = new AccountPrincipal(users.get(role));
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
            )
        );
        SecurityContextHolder.setContext(context);
        try {
            return action.call();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    private MockHttpSession login(Role role) throws Exception {
        MvcResult anonymous = mvc.perform(get("/api/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) anonymous.getRequest().getSession(false);
        JsonNode token = json.readTree(anonymous.getResponse().getContentAsString());
        mvc.perform(
            post("/api/auth/login")
                .session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .param("email", users.get(role).getEmail())
                .param("password", "TestPass123")
        ).andExpect(status().isOk());

        return session;
    }

    private JsonNode token(MockHttpSession session) throws Exception {
        return json.readTree(
            mvc
                .perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
        );
    }

    private String body(String name, String email, String password) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>(Map.of("fullName", name, "email", email));
        if (password != null) {
            fields.put("password", password);
        }

        return json.writeValueAsString(fields);
    }

    private static String route(Role role) {
        return (
            "/api/" +
            switch (role) {
                case ADMIN -> "admins";
                case TEACHER -> "teachers";
                case STUDENT -> "students";
            }
        );
    }
}
