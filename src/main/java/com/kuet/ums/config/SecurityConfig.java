package com.kuet.ums.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.ums.dto.AccountResponse;
import com.kuet.ums.repository.AccountRepository;
import com.kuet.ums.security.*;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(AccountDetailsService details, PasswordEncoder passwords) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(passwords);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider,
            AccountRepository accounts, ObjectMapper json, SecurityErrorWriter errors) throws Exception {
        http.authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/", "/index.html", "/login.html", "/management.html", "/css/**", "/js/**", "/favicon.svg", "/actuator/health", "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/courses", "/api/courses/**", "/api/notices", "/api/notices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/courses", "/api/courses/**", "/api/notices", "/api/notices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/courses", "/api/courses/**", "/api/notices", "/api/notices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/enrollments", "/api/enrollments/**").hasAnyRole("ADMIN", "STUDENT")
                .requestMatchers(HttpMethod.DELETE, "/api/enrollments", "/api/enrollments/**").hasAnyRole("ADMIN", "STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/teachers", "/api/teachers/**", "/api/students", "/api/students/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/teachers", "/api/teachers/**", "/api/students", "/api/students/**").hasRole("ADMIN")
                .requestMatchers("/api/admins", "/api/admins/**").hasRole("ADMIN")
                .requestMatchers("/api/teachers", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/students", "/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                .anyRequest().authenticated())
            .csrf(Customizer.withDefaults())
            .requestCache(cache -> cache.disable())
            .exceptionHandling(handler -> handler
                .authenticationEntryPoint((request, response, ex) -> errors.write(response, 401, "UNAUTHENTICATED", "Please sign in to continue."))
                .accessDeniedHandler((request, response, ex) -> errors.write(response, 403,
                    ex instanceof CsrfException ? "CSRF_ERROR" : "FORBIDDEN",
                    ex instanceof CsrfException ? "Your security token is missing or expired. Refresh and try again." : "Your role cannot access this resource.")))
            .formLogin(login -> login.loginPage("/login.html").loginProcessingUrl("/api/auth/login")
                .usernameParameter("email").permitAll()
                .successHandler((request, response, authentication) -> {
                    AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(response.getOutputStream(), AccountResponse.from(accounts.findById(principal.getId()).orElseThrow()));
                })
                .failureHandler((request, response, ex) -> errors.write(response, 401, "INVALID_CREDENTIALS", "Invalid email or password.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).clearAuthentication(true)
                .deleteCookies("JSESSIONID").logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'")))
            .addFilterBefore(new ActiveAccountFilter(accounts, errors), AuthorizationFilter.class);
        return http.build();
    }
}
