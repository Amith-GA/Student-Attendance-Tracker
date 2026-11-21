package com.attendance.tracker.security;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs on every request, after Spring Security has found out who is logged in.
 *
 * Why it is needed: the login check (AppUserDetailsService) only happens when someone logs in.
 * A person who was ALREADY logged in keeps their session, so without this filter
 *  - a student deactivated by the admin could keep using the site, and
 *  - a student deleted by the admin would get an error page.
 * This filter looks the user up again; if the account is gone or deactivated,
 * the session is ended and the user is sent back to the login page.
 */
public class ActiveUserFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public ActiveUserFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        if (loggedIn) {
            AppUser user = users.findByUsername(auth.getName()).orElse(null);
            if (user == null || !user.isActive()) {
                // end the session, then send the person to the login page
                new SecurityContextLogoutHandler().logout(request, response, auth);
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
