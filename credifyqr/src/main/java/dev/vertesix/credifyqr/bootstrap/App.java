package dev.vertesix.credifyqr.bootstrap;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;

import dev.vertesix.credifyqr.Identity.adapters.inbound.web.AuthController;
import dev.vertesix.credifyqr.Identity.adapters.inbound.web.AuthMiddleware;
import dev.vertesix.credifyqr.Identity.adapters.inbound.web.PageController;
import dev.vertesix.credifyqr.Identity.adapters.outbound.db.SqliteUserRepository;
import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import dev.vertesix.credifyqr.Identity.core.service.IdentityService;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;


public class App {

    public static void main(String[] args) {
        
        // 1. Initialize the Outbound Adapter (Database)
        String dbUrl = "jdbc:sqlite:credifyqr.db";
        UserRepository userRepository = new SqliteUserRepository(dbUrl);

        // 2. Initialize the Core Service (Domain Logic)
        IdentityUseCase identityService = new IdentityService(userRepository);

        // 3. Initialize the Inbound Adapter (Web Controller)
        AuthController authController = new AuthController(identityService);
        PageController pageController = new PageController(identityService);


        // 4. Start the Javalin Web Framework
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.fileRenderer(new JavalinThymeleaf(createTemplateEngine()));
            // Static resources (CSS/JS)
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
        }).start(5555);

        // 5. Register Public Auth Routes
        authController.registerRoutes(app);
        pageController.registerRoutes(app); 

        // Protected: Student Only
        app.before("/api/student/*", ctx -> AuthMiddleware.requireRole(ctx, Role.STUDENT));
        app.get("/api/student/dashboard", ctx -> {
            ctx.result("Access Granted: Welcome to the Student Dashboard.");
        });

        // Admin routes (Registrar or Director)
        app.before("/api/admin/*", ctx -> AuthMiddleware.requireRole(ctx, Role.REGISTRAR_STAFF, Role.CAMPUS_DIRECTOR));
        app.get("/api/admin/credentials", ctx -> {
            ctx.result("Access Granted: Credential Management Area.");
        });

        System.out.println("CredifyQR Identity Service running on http://localhost:5555");
        
        try {
            identityService.registerUser("director_admin", "password123", "CAMPUS_DIRECTOR");
            identityService.registerUser("registrar_staff", "password123", "REGISTRAR_STAFF");
            identityService.registerUser("student_01", "password123", "STUDENT");
            System.out.println("Test accounts seeded.");
        } catch (IllegalArgumentException e) {
            //
        }
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/thymeleaf/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}