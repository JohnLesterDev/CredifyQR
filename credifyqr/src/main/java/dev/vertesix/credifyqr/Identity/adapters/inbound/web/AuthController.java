package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import java.util.Map;
import java.util.Optional;

import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import dev.vertesix.credifyqr.Identity.core.ports.TokenBlacklistRepository;
import dev.vertesix.credifyqr.Identity.core.security.SanitizerUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.jsonwebtoken.Claims;

public class AuthController {

    private final IdentityUseCase identityUseCase;
    private final TokenBlacklistRepository blacklistRepository;

    public AuthController(IdentityUseCase identityUseCase, TokenBlacklistRepository blacklistRepository) {
        this.identityUseCase = identityUseCase;
        this.blacklistRepository = blacklistRepository;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/login", this::login);
        app.post("/api/logout", this::logout);
        app.get("/api/session", this::session);
        app.post("/api/claim-account", this::claimAccount);
        app.post("/api/change-password", this::changePassword);
    }

    private void login(Context ctx) {
        String username = SanitizerUtil.clean(ctx.formParam("username"));
        String password = ctx.formParam("password");
        boolean rememberMe = Boolean.parseBoolean(ctx.formParam("rememberMe"));

        if (username == null || password == null) {
            ctx.status(400).result("Missing credentials");
            return;
        }

        Optional<User> authenticatedUser = identityUseCase.authenticate(username, password);

        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            String token = JwtProvider.createToken(user.getId(), user.getRole().name(), rememberMe);
            
            Cookie jwtCookie = new Cookie("auth_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // Set to TRUE when SSL/HTTPS is active
            jwtCookie.setSameSite(SameSite.STRICT);
            jwtCookie.setPath("/");
            
            // If rememberMe is true, persist the cookie for 30 days (in seconds).
            // Otherwise, do not set maxAge, making it a transient session cookie.
            if (rememberMe) {
                jwtCookie.setMaxAge(30 * 24 * 60 * 60); 
            }
            
            ctx.cookie(jwtCookie);
            ctx.status(200).result("Login successful");
        } else {
            ctx.status(401).result("Invalid credentials");
        }
    }

    private void logout(Context ctx) {
        String token = ctx.cookie("auth_token");
        if (token != null) {
            try {
                Claims claims = JwtProvider.validateToken(token);
                blacklistRepository.blacklist(claims.getId(), claims.getExpiration().getTime());
            } catch (Exception ignored) {
                // TODO:
            }
        }
        ctx.removeCookie("auth_token");
        ctx.status(200).result("Logged out and session revoked.");
    }

    private void session(Context ctx) {
        String token = ctx.cookie("auth_token");
        if (token == null) {
            ctx.status(401).json("Not authenticated");
            return;
        }
        try {
            Claims claims = JwtProvider.validateToken(token);
            Optional<User> userOpt = identityUseCase.findById(claims.getSubject());
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                ctx.json(Map.of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "role", u.getRole().name()
                ));
            }
        } catch (Exception e) {
            ctx.removeCookie("auth_token");
            ctx.status(401).json("Session invalid");
        }
    }

    private void claimAccount(Context ctx) {
        String studentId = SanitizerUtil.clean(ctx.formParam("studentId"));
        String birthdate = SanitizerUtil.clean(ctx.formParam("birthdate"));

        if (studentId == null || birthdate == null) {
            ctx.status(400).result("Missing verification details.");
            return;
        }

        try {
            String tempPassword = identityUseCase.claimStudentAccount(studentId, birthdate);
            ctx.status(200).result(tempPassword); 
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Internal error.");
        }
    }

    public void changePassword(Context ctx) {
        String userId = ctx.attribute("userId"); 
        String newPassword = ctx.formParam("newPassword");

        if (userId == null) {
            ctx.status(401).result("Unauthorized.");
            return;
        }

        if (newPassword == null || newPassword.isBlank()) {
            ctx.status(400).result("Password cannot be empty.");
            return;
        }

        try {
            identityUseCase.changePassword(userId, newPassword);
            ctx.status(200).result("Password updated successfully.");
        } catch (Exception e) {
            ctx.status(500).result("Update failed.");
        }
    }
}