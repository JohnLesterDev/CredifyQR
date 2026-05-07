package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PageController {
    private final IdentityUseCase identityUseCase;

    public PageController(IdentityUseCase identityUseCase) {
        this.identityUseCase = identityUseCase;
    }

    public void registerRoutes(Javalin app) {
        // Landing page redirects based on session
        app.get("/", this::landing);
        app.get("/login", this::showLogin);
        app.get("/dashboard", this::showDashboard);
    }

    private void landing(Context ctx) {
        String userId = ctx.cookie("auth_session");
        if (isValidSession(userId)) {
            ctx.redirect("/dashboard");
        } else {
            ctx.redirect("/login");
        }
    }

    private void showLogin(Context ctx) {
        // If already logged in, go to dashboard
        if (isValidSession(ctx.cookie("auth_session"))) {
            ctx.redirect("/dashboard");
            return;
        }
        ctx.render("login");  // Thymeleaf template
    }

    private void showDashboard(Context ctx) {
        String userId = ctx.cookie("auth_session");
        if (userId == null || !isValidSession(userId)) {
            ctx.redirect("/login");
            return;
        }
        Optional<User> userOpt = identityUseCase.findById(userId);
        if (userOpt.isEmpty()) {
            ctx.removeCookie("auth_session");
            ctx.removeCookie("user_role");
            ctx.redirect("/login");
            return;
        }
        User user = userOpt.get();
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("role", user.getRole().name());
        ctx.render("dashboard", model);
    }

    private boolean isValidSession(String userId) {
        if (userId == null || userId.isBlank()) return false;
        return identityUseCase.findById(userId).isPresent();
    }
}