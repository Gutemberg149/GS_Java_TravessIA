package br.com.travessia.comum.seguranca;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil() {
        String rawSecret = System.getenv().getOrDefault(
                "JWT_SECRET",
                "TravessIASecretKeyForHS256AlgorithmMinimum256BitsRequired2026Fiap"
        );
        this.signingKey = deriveKey(rawSecret);
        this.expirationMs = Long.parseLong(System.getenv().getOrDefault("JWT_EXPIRATION_MS", "31536000000"));
    }

    private static SecretKey deriveKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 not available", e);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String gerarToken(String username, String papel) {
        Date agora = new Date();
        Date validade = new Date(agora.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .claim("role", papel)
                .setIssuedAt(agora)
                .setExpiration(validade)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extrairUsuario(String token) {
        return extrairClaims(token).getSubject();
    }

    public String extrairPapel(String token) {
        return extrairClaims(token).get("role", String.class);
    }

    public boolean validarToken(String token, String username) {
        return extrairUsuario(token).equals(username) && !extrairClaims(token).getExpiration().before(new Date());
    }
}
