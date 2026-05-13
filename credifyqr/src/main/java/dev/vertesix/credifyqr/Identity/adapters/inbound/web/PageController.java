package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.jsonwebtoken.Claims;

public class PageController {
    private final IdentityUseCase identityUseCase;

    public PageController(IdentityUseCase identityUseCase) {
        this.identityUseCase = identityUseCase;
    }

    public void registerRoutes(Javalin app) {
        app.get("/", this::landing);
        app.get("/login", this::showLogin);
        app.get("/admin", this::showAdminLogin);
        app.get("/dashboard", this::showDashboard);
        app.get("/force-password-change", this::showForcePasswordChange);
        
        // NEW: Employer/Scanner UI Routing
        app.get("/verify/{id}", this::showVerifyPage);
    }

    private String getUserIdFromToken(Context ctx) {
        String token = ctx.cookie("auth_token"); 
        if (token == null || token.isBlank()) return null;
        try {
            Claims claims = JwtProvider.validateToken(token);
            return claims.getSubject(); 
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidSession(Context ctx) {
        String userId = getUserIdFromToken(ctx);
        if (userId == null) return false;
        return identityUseCase.findById(userId).isPresent();
    }

    private void landing(Context ctx) {
        if (isValidSession(ctx)) {
            ctx.redirect("/dashboard");
        } else {
            String pref = ctx.cookie("portal_pref");
            if ("admin".equals(pref)) {
                ctx.redirect("/admin");
            } else {
                ctx.redirect("/login");
            }
        }
    }

    private void showLogin(Context ctx) {        
        if (isValidSession(ctx)) {
            ctx.redirect("/dashboard");
            return;
        }
        String nonce = java.util.UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> model = new HashMap<>();
        model.put("nonce", nonce);
        ctx.render("login", model);
    }

    private void showDashboard(Context ctx) {
        String userId = getUserIdFromToken(ctx);
        if (userId == null) {
            ctx.removeCookie("auth_token");
            ctx.redirect("/");
            return;
        }

        Optional<User> userOpt = identityUseCase.findById(userId);
        if (userOpt.isEmpty()) {
            ctx.removeCookie("auth_token");
            ctx.redirect("/");
            return;
        }
        User user = userOpt.get();

        if (user.isTemporary()) {
            ctx.redirect("/force-password-change");
            return;
        }

        Map<String, Object> model = new HashMap<>();
        // Injected fullName for UI display
        model.put("fullName", user.getFullName());
        model.put("username", user.getUsername());
        model.put("role", user.getRole().name());
        ctx.render("dashboard", model);
    }

    private void showForcePasswordChange(Context ctx) {
        String userId = getUserIdFromToken(ctx);
        if (userId == null) {
            ctx.redirect("/");
            return;
        }

        Optional<User> userOpt = identityUseCase.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().needsPasswordReset()) {
            ctx.redirect("/dashboard");
            return;
        }

        ctx.render("force-password-change");
    }

    private void showAdminLogin(Context ctx) {        
        if (isValidSession(ctx)) {
            ctx.redirect("/dashboard");
            return;
        }
        String nonce = java.util.UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> model = new HashMap<>();
        model.put("nonce", nonce);
        ctx.render("admin-login", model);
    }

    private void showVerifyPage(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("docId", ctx.pathParam("id"));
        ctx.render("verify", model);
    }
}