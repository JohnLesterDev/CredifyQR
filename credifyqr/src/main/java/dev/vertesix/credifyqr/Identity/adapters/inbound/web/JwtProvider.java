package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.*;

public class JwtProvider {
    private static SecretKey KEY;

    // Call this exactly once during App startup
    public static void init(String secret) {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters long.");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public static String createToken(String userId, String role) {
        if (KEY == null) throw new IllegalStateException("JwtProvider not initialized with a secret.");
        
        return Jwts.builder()
            .id(UUID.randomUUID().toString()) 
            .subject(userId)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .signWith(KEY)
            .compact();
    }

    public static Claims validateToken(String token) {
        if (KEY == null) throw new IllegalStateException("JwtProvider not initialized with a secret.");
        return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    }
}