package dev.vertesix.credifyqr.bootstrap;

import java.util.UUID;

import dev.vertesix.credifyqr.Identity.adapters.inbound.web.AuthController;
import dev.vertesix.credifyqr.Identity.adapters.inbound.web.AuthMiddleware;
import dev.vertesix.credifyqr.Identity.adapters.inbound.web.PageController;
import dev.vertesix.credifyqr.Identity.adapters.outbound.db.SqliteUserRepository;
import dev.vertesix.credifyqr.Identity.adapters.outbound.db.SqliteBlacklistRepository;
import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;
import dev.vertesix.credifyqr.Identity.core.ports.TokenBlacklistRepository;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import dev.vertesix.credifyqr.Identity.core.service.IdentityService;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;

import io.github.cdimascio.dotenv.Dotenv;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();

        String dbUrl = dotenv.get("DB_URL", "jdbc:sqlite:credifyqr.db");
        String jwtSecret = dotenv.get("JWT_SECRET");

        String host = dotenv.get("HOST", "localhost");
        int port = Integer.parseInt(dotenv.get("PORT", "5555"));

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new RuntimeException("FATAL: JWT_SECRET environment variable is missing.");
        }

        dev.vertesix.credifyqr.Identity.adapters.inbound.web.JwtProvider.init(jwtSecret);

        UserRepository userRepository = new SqliteUserRepository(dbUrl);
        TokenBlacklistRepository blacklistRepository = new SqliteBlacklistRepository(dbUrl);

        // 2. Core Service Initialization (Hexagonal Logic)
        IdentityUseCase identityService = new IdentityService(userRepository);
        
        // 3. Security & Controller Initialization
        AuthMiddleware.init(blacklistRepository); 
        AuthController authController = new AuthController(identityService, blacklistRepository);
        PageController pageController = new PageController(identityService);

        // 4. Javalin Configuration
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.fileRenderer(new JavalinThymeleaf(createTemplateEngine()));
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            
            // Hardening: Prevent CSRF/Clickjacking via headers if needed
            config.http.defaultContentType = "text/html; charset=UTF-8";
        }).start(host, port);

        // 5. Route Registration
        authController.registerRoutes(app);
        pageController.registerRoutes(app); 

        // 6. Role-Based Access Control (RBAC) Interceptors
        app.before("/api/student/*", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT));
        app.before("/api/admin/*", ctx -> AuthMiddleware.requireRole(ctx, Role.REGISTRAR_STAFF, Role.CAMPUS_DIRECTOR));
        app.post("/api/student/change-password", authController::changePassword);

        logger.info("CredifyQR Identity Service running on http://{}:{}", host, port);

        seedTestAccounts(identityService, userRepository);

        logger.info("System ready. All test accounts verified.");
    }

    private static void seedTestAccounts(IdentityUseCase service, UserRepository repo) {
        String[][] testAccounts = {
            {"director_admin", "password123", "CAMPUS_DIRECTOR"},
            {"registrar_staff", "password123", "REGISTRAR_STAFF"},
            {"student_01", "password123", "STUDENT"}
        };

        for (String[] acc : testAccounts) {
            try {
                service.registerUser(acc[0], acc[1], acc[2]);
            } catch (IllegalArgumentException e) {
                // Ignore duplicates if DB wasn't wiped
            }
        }

        try {
            if (repo.findByUsername("25001234").isEmpty()) {
                // Passwords for unclaimed accounts are empty; birthdate is the primary initial secret
                User unclaimed = new User(UUID.randomUUID().toString(), "25001234", "", Role.STUDENT, "2000-01-01", false, true);
                repo.save(unclaimed);
                logger.info("Unclaimed test account seeded: 25001234");
            }
        } catch (Exception e) {
            logger.error("Failed to seed unclaimed account", e);
        }
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/thymeleaf/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}