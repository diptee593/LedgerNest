package com.ledgernest.security;

import io.jsonwebtoken.Claims; //imports the Claims interface from the popular Java JWT (JJWT) library. This interface is used to read, verify, or extract payload data from a JSON Web Token (JWT) in Java or Spring Boot
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys; // to create cryptographic keys 
import org.springframework.beans.factory.annotation.Value; // Reading simple values from property files.
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil{
    // Pulled from application.properties → jwt.secret
    @Value("${jwt.secret}")
    private String secret;


    // Pulled from application.properties → jwt.expiration (86400000 = 24 hours
    @Value("${jwt.expiration}")
    private String expiration;

     private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
     // Creates a JWT token at login time
    // We store email as the subject + tenantId as a custom claim
    // tenantId inside the token = every request automatically carries company info

    public String generateToken(String email, String tenantId) {
        return Jwts.builder()
                .subject(email)
                .claim("tenantId", tenantId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }


    // Core internal method — parses the token and returns its payload
    // If token is expired or tampered → Jwts.parser() throws an exception automatically
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    // Used by JwtFilter to identify who is making the request
    public String extractEmail(String token){
        return extractAllClaims(token).getSubject();
    }

    // Used in services to scope all DB queries to the right company
     public String extractTenantId(String token) {
        return extractAllClaims(token).get("tenantId", String.class);
    }

    // Final validity check — is the token parseable AND not expired?
    // Any tampering with the token makes parsing throw an exception → returns false
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
