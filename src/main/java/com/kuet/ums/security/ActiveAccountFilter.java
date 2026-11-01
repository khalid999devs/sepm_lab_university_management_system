package com.kuet.ums.security;

import com.kuet.ums.repository.AccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveAccountFilter extends OncePerRequestFilter {

    private final AccountRepository accounts;
    private final SecurityErrorWriter errors;

    public ActiveAccountFilter(AccountRepository accounts, SecurityErrorWriter errors) {
        this.accounts = accounts;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        return !path.startsWith("/api/") || path.equals("/api/auth/csrf");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (
            authentication != null &&
            authentication.getPrincipal() instanceof AccountPrincipal principal &&
            !accounts.existsById(principal.getId())
        ) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            SecurityContextHolder.clearContext();
            errors.write(
                response,
                401,
                "UNAUTHENTICATED",
                "This account no longer exists. Please sign in again."
            );

            return;
        }

        chain.doFilter(request, response);
    }
}
