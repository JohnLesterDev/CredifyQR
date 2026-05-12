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

// Wired SqliteAuditRepository into bootstrap and injected into IdentityService
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        String dbUrl = dotenv.get("DB_URL", "jdbc:sqlite:credifyqr.db");
        String jwtSecret = dotenv.get("JWT_SECRET");

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new RuntimeException("FATAL: JWT_SECRET is missing.");
        }

        DatabaseConnection.init(dbUrl);
        JwtProvider.init(jwtSecret);

        UserRepository userRepository = new SqliteUserRepository();
        SettingsRepository settingsRepository = new SqliteSettingsRepository();
        TokenBlacklistRepository blacklistRepository = new SqliteBlacklistRepository();
        PasswordGenerator passwordGenerator = new SecurePasswordAdapter();
        AuditRepository auditRepository = new SqliteAuditRepository();

        IdentityUseCase identityService = new IdentityService(userRepository, settingsRepository, passwordGenerator, auditRepository);
        AuthMiddleware.init(blacklistRepository); 
        AuthController authController = new AuthController(identityService, blacklistRepository);
        PageController pageController = new PageController(identityService);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.fileRenderer(new JavalinThymeleaf(createTemplateEngine()));
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            config.http.defaultContentType = "text/html; charset=UTF-8";
        }).start(dotenv.get("HOST", "localhost"), Integer.parseInt(dotenv.get("PORT", "5555")));

        authController.registerRoutes(app);
        pageController.registerRoutes(app); 

        app.before("/api/student/*", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT));
        app.before("/api/change-password", ctx -> AuthMiddleware.requireRole(ctx, Role.values()));
        
        app.before("/api/admin/settings/*", ctx -> AuthMiddleware.requireRole(ctx, Role.SYSTEM_ADMIN));
        
        app.before("/api/admin/users", ctx -> AuthMiddleware.requireRole(ctx, Role.SYSTEM_ADMIN, Role.REGISTRAR_STAFF, Role.CAMPUS_DIRECTOR));

        app.before("/api/admin/users/approve", ctx -> AuthMiddleware.requireRole(ctx, Role.CAMPUS_DIRECTOR));

        seedSystem(identityService, userRepository);

        logger.info("CredifyQR Overhaul Complete. System is live.");
    }

    private static void seedSystem(IdentityUseCase service, UserRepository repo) {
        if (repo.findByUsername("sysadmin").isEmpty()) {
            User sysAdmin = new User(
                UUID.randomUUID().toString(), 
                "sysadmin", 
                "sysadmin@vertesix.dev", 
                org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt(12)), 
                Role.SYSTEM_ADMIN, 
                "2000-01-01",
                "System",      
                "Administrator", 
                "V",             
                true, false, true, true,
                0, 0L 
            );
            repo.save(sysAdmin);
            logger.info("SYSTEM_ADMIN seeded. User: sysadmin");
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