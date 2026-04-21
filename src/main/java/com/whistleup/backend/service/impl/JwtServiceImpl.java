package com.whistleup.backend.service.impl;

import com.whistleup.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtServiceImpl(
            @Value("${jwt.secret:${JWT_SECRET:whistleup-local-dev-jwt-secret-key-32-bytes-minimum}}") String configuredSecret,
            @Value("${jwt.expiration-ms:${JWT_EXPIRATION_MS:86400000}}") long expirationMs) {
        this.signingKey = toSigningKey(configuredSecret);
        this.expirationMs = expirationMs > 0 ? expirationMs : 86_400_000L;
    }

    @Override
    public String generateToken(String userName) {
        Map<String,Object> claims = new HashMap<>();

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(userName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .and()
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String extractToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsTFunction) {
       final Claims claims = extractAllClaims(token);
       return claimsTFunction.apply(claims);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String userName = extractToken(token);
            return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private SecretKey toSigningKey(String rawSecret) {
        String secret = rawSecret == null ? "" : rawSecret.trim();
        if (secret.isEmpty()) {
            throw new IllegalStateException("JWT secret must not be empty.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ignore) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
