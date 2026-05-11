package dev.vertesix.credifyqr.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import dev.vertesix.credifyqr.Identity.adapters.inbound.web.*;
import dev.vertesix.credifyqr.Identity.adapters.outbound.db.*;
import dev.vertesix.credifyqr.Identity.adapters.outbound.security.SecurePasswordAdapter;
import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.*;
import dev.vertesix.credifyqr.Identity.core.service.IdentityService;

import java.util.UUID;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        String dbUrl = dotenv.get("DB_URL", "jdbc:sqlite:credifyqr.db");
        String jwtSecret = dotenv.get("JWT_SECRET");

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new RuntimeException("FATAL: JWT_SECRET is missing.");
        }

        // 1. Initialize Infrastructure
        DatabaseConnection.init(dbUrl);
        JwtProvider.init(jwtSecret);

        // 2. Initialize Adapters (No longer passing dbUrl)
        UserRepository userRepository = new SqliteUserRepository();
        SettingsRepository settingsRepository = new SqliteSettingsRepository();
        TokenBlacklistRepository blacklistRepository = new SqliteBlacklistRepository();
        PasswordGenerator passwordGenerator = new SecurePasswordAdapter();

        // 3. Core Service & Controller Initialization
        IdentityUseCase identityService = new IdentityService(userRepository, settingsRepository, passwordGenerator);
        AuthMiddleware.init(blacklistRepository); 
        AuthController authController = new AuthController(identityService, blacklistRepository);
        PageController pageController = new PageController(identityService);

        // 4. Javalin Setup
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.fileRenderer(new JavalinThymeleaf(createTemplateEngine()));
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            config.http.defaultContentType = "text/html; charset=UTF-8";
        }).start(dotenv.get("HOST", "localhost"), Integer.parseInt(dotenv.get("PORT", "5555")));

        // 5. Routes
        authController.registerRoutes(app);
        pageController.registerRoutes(app); 

        // 6. RBAC Overhaul
        app.before("/api/student/*", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT));
        app.before("/api/change-password", ctx -> AuthMiddleware.requireRole(ctx, Role.values()));
        
        // SysAdmin restricted routes
        app.before("/api/admin/settings/*", ctx -> AuthMiddleware.requireRole(ctx, Role.SYSTEM_ADMIN));
        
        // Provisioning logic: Staff creation (SysAdmin only), Student creation (Registrar/SysAdmin)
        app.before("/api/admin/users", ctx -> AuthMiddleware.requireRole(ctx, Role.SYSTEM_ADMIN, Role.REGISTRAR_STAFF));

        seedSystem(identityService, userRepository);

        logger.info("CredifyQR Overhaul Complete. System is live.");
    }

    private static void seedSystem(IdentityUseCase service, UserRepository repo) {
        // Seed the MASTER SysAdmin if it doesn't exist
        if (repo.findByUsername("admin_root").isEmpty()) {
            User sysAdmin = new User(
                UUID.randomUUID().toString(), 
                "admin_root", 
                "sysadmin@credify.edu.ph", 
                org.mindrot.jbcrypt.BCrypt.hashpw("root1234", org.mindrot.jbcrypt.BCrypt.gensalt(12)), 
                Role.SYSTEM_ADMIN, 
                "2000-01-01", 
                true, false, true
            );
            repo.save(sysAdmin);
            logger.info("SYSTEM_ADMIN seeded. User: admin_root | Pwd: root1234");
        }

        // Restore the unclaimed student for the verification test pipeline
        if (repo.findByUsername("25001234").isEmpty()) {
            User unclaimedStudent = new User(
                UUID.randomUUID().toString(),
                "25001234",
                null, // Email is null; not strictly required for Student claims
                "",   // Empty password hash; will be generated during claim
                Role.STUDENT,
                "2000-01-01",
                false, // isClaimed
                true,  // needsPasswordReset
                true   // isActive
            );
            repo.save(unclaimedStudent);
            logger.info("Unclaimed test account seeded: 25001234 | DOB: 2000-01-01");
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