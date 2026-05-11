package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import java.util.Date;
import java.util.UUID;


public class JwtProvider {
    private static SecretKey KEY;

    // Call this exactly once during App startup
    public static void init(String secret) {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters long.");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public static String createToken(String userId, String role, boolean rememberMe) {
        if (KEY == null) throw new IllegalStateException("JwtProvider not initialized with a secret.");
        
        // 30 days if remembered, 1 hour if not.
        long expirationMillis = rememberMe ? 30L * 24 * 60 * 60 * 1000 : 3600000L;
        
        return Jwts.builder()
            .id(UUID.randomUUID().toString()) 
            .subject(userId)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMillis))
            .signWith(KEY)
            .compact();
    }

    public static Claims validateToken(String token) {
        if (KEY == null) throw new IllegalStateException("JwtProvider not initialized with a secret.");
        return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    }
}