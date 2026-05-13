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

import dev.vertesix.credifyqr.Credentials.adapters.inbound.web.DocumentController;
import dev.vertesix.credifyqr.Credentials.adapters.outbound.db.SqliteDocumentRepository;
import dev.vertesix.credifyqr.Credentials.adapters.outbound.pdf.PdfBoxStamperAdapter;
import dev.vertesix.credifyqr.Credentials.core.ports.DocumentRepository;
import dev.vertesix.credifyqr.Credentials.core.ports.DocumentUseCase;
import dev.vertesix.credifyqr.Credentials.core.ports.PdfStamperPort;
import dev.vertesix.credifyqr.Credentials.core.service.DocumentService;

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

        DatabaseConnection.init(dbUrl);
        JwtProvider.init(jwtSecret);

        // Identity Context
        UserRepository userRepository = new SqliteUserRepository();
        SettingsRepository settingsRepository = new SqliteSettingsRepository();
        TokenBlacklistRepository blacklistRepository = new SqliteBlacklistRepository();
        PasswordGenerator passwordGenerator = new SecurePasswordAdapter();
        AuditRepository auditRepository = new SqliteAuditRepository();
        IdentityUseCase identityService = new IdentityService(userRepository, settingsRepository, passwordGenerator, auditRepository);
        
        // Credentials Context
        DocumentRepository documentRepository = new SqliteDocumentRepository();
        PdfStamperPort pdfStamperPort = new PdfBoxStamperAdapter();
        DocumentUseCase documentService = new DocumentService(documentRepository, pdfStamperPort, userRepository, auditRepository);

        AuthMiddleware.init(blacklistRepository); 
        AuthController authController = new AuthController(identityService, blacklistRepository);
        PageController pageController = new PageController(identityService);
        DocumentController documentController = new DocumentController(documentService, identityService);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.fileRenderer(new JavalinThymeleaf(createTemplateEngine()));
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            config.http.defaultContentType = "text/html; charset=UTF-8";
        }).start(dotenv.get("HOST", "localhost"), Integer.parseInt(dotenv.get("PORT", "5555")));

        authController.registerRoutes(app);
        pageController.registerRoutes(app); 
        documentController.registerRoutes(app);

        // Routing Security Enforcements
        app.before("/api/student/*", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT));
        app.before("/api/change-password", ctx -> AuthMiddleware.requireRole(ctx, Role.values()));
        app.before("/api/admin/settings/*", ctx -> AuthMiddleware.requireRole(ctx, Role.SYSTEM_ADMIN));
        app.before("/api/admin/users", ctx -> AuthMiddleware.requireRole(ctx, Role.SYSTEM_ADMIN, Role.REGISTRAR_STAFF, Role.CAMPUS_DIRECTOR));
        app.before("/api/admin/users/approve", ctx -> AuthMiddleware.requireRole(ctx, Role.CAMPUS_DIRECTOR));

        // Credentials Context Security
        app.before("/api/docs/request", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT));
        app.before("/api/docs/student", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT));
        app.before("/api/docs/registrar/*", ctx -> AuthMiddleware.requireRole(ctx, Role.REGISTRAR_STAFF));
        app.before("/api/docs/director/*", ctx -> AuthMiddleware.requireRole(ctx, Role.CAMPUS_DIRECTOR));
        app.before("/api/docs/*/upload", ctx -> AuthMiddleware.requireRole(ctx, Role.REGISTRAR_STAFF));
        app.before("/api/docs/*/approve", ctx -> AuthMiddleware.requireRole(ctx, Role.CAMPUS_DIRECTOR));
        app.before("/api/docs/*/download", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT, Role.REGISTRAR_STAFF, Role.CAMPUS_DIRECTOR, Role.SYSTEM_ADMIN));

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