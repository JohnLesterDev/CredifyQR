package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;

import java.util.Arrays;

public class AuthMiddleware {

    /**
     * Intercepts requests to enforce Role-Based Access Control (RBAC).
     * Call this at the start of any protected route handler.
     */
    public static void requireRole(Context ctx, Role... allowedRoles) {
        String userRoleStr = ctx.cookie("user_role");

        // 1. Verify a session actually exists
        if (userRoleStr == null || userRoleStr.isBlank()) {
            throw new UnauthorizedResponse("Missing or invalid session. Authentication required.");
        }

        try {
            // 2. Parse the role from the cookie
            Role userRole = Role.valueOf(userRoleStr);
            
            // 3. Check if the user's role is in the permitted list
            boolean authorized = Arrays.asList(allowedRoles).contains(userRole);
            if (!authorized) {
                throw new ForbiddenResponse("Access Denied: Your current role (" + userRole + ") lacks the required privileges for this action.");
            }
            
        } catch (IllegalArgumentException e) {
            // If the cookie was manipulated or corrupted with a non-existent role string
            ctx.removeCookie("user_role");
            ctx.removeCookie("auth_session");
            throw new UnauthorizedResponse("Session data corrupted. Please log in again.");
        }
    }
}