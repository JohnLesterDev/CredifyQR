package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.*;

public class AuthController {

    private final IdentityUseCase identityUseCase;

    // Dependency Injection: The controller only knows about the UseCase interface.
    public AuthController(IdentityUseCase identityUseCase) {
        this.identityUseCase = identityUseCase;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/login", this::login);
        app.post("/api/register", this::register);
        app.post("/api/logout", this::logout);
        app.get("/api/session", this::session);
    }

    private void login(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (username == null || password == null) {
            ctx.status(400).result("Missing credentials");
            return;
        }

        Optional<User> authenticatedUser = identityUseCase.authenticate(username, password);

        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            ctx.cookie("auth_session", user.getId()); 
            ctx.cookie("user_role", user.getRole().name());
            ctx.status(200).result("Login successful. Role: " + user.getRole().name());
        } else {
            ctx.status(401).result("Invalid credentials");
        }
    }

    private void register(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String role = ctx.formParam("role"); // e.g., STUDENT, REGISTRAR_STAFF

        if (username == null || password == null || role == null) {
            ctx.status(400).result("Missing registration details");
            return;
        }

        try {
            identityUseCase.registerUser(username, password, role);
            ctx.status(201).result("User registered successfully");
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Internal server error during registration");
        }
    }

    private void logout(Context ctx) {
        ctx.removeCookie("auth_session");
        ctx.removeCookie("user_role");
        ctx.status(200).result("Logged out");
    }

    private void session(Context ctx) {
    String userId = ctx.cookie("auth_session");
    if (userId == null || userId.isBlank()) {
        ctx.status(401).json("Not authenticated");
        return;
    }
    Optional<User> userOpt = identityUseCase.findById(userId);
    if (userOpt.isPresent()) {
        User u = userOpt.get();
        ctx.json(Map.of(
            "id", u.getId(),
            "username", u.getUsername(),
            "role", u.getRole().name()
        ));
    } else {
        ctx.removeCookie("auth_session");
        ctx.removeCookie("user_role");
        ctx.status(401).json("Session invalid");
    }
}
}
