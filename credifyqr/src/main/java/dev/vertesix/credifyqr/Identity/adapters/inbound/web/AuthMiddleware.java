package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.ports.TokenBlacklistRepository;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.jsonwebtoken.Claims;
import java.util.Arrays;

public class AuthMiddleware {
    private static TokenBlacklistRepository blacklist;

    public static void init(TokenBlacklistRepository repo) { blacklist = repo; }

    public static void requireRole(Context ctx, Role... allowedRoles) {
        String token = ctx.cookie("auth_token");
        if (token == null) throw new UnauthorizedResponse("Authentication required.");

        try {
            Claims claims = JwtProvider.validateToken(token);
            String jti = claims.getId();

            if (blacklist.isBlacklisted(jti)) {
                throw new UnauthorizedResponse("Session revoked.");
            }

            Role userRole = Role.valueOf(claims.get("role", String.class));
            if (!Arrays.asList(allowedRoles).contains(userRole)) {
                throw new io.javalin.http.ForbiddenResponse("Access Denied.");
            }

            ctx.attribute("userId", claims.getSubject());
        } catch (Exception e) {
            ctx.removeCookie("auth_token");
            throw new UnauthorizedResponse("Invalid session.");
        }
    }
}