package com.literandltx.timer.security;

import com.literandltx.timer.config.env.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final JwtConfig jwtConfig;

    private final SecretKey secret;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secret = Keys.hmacShaKeyFor(jwtConfig.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, long expirationMillis, TokenType tokenType) {
        return Jwts.builder()
                .subject(username)
                .claim("token_type", tokenType.name())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secret)
                .compact();
    }

    public String generateAccessToken(String username) {
        return generateToken(username, jwtConfig.expiration(), TokenType.ACCESS);
    }

    public boolean isValidToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(token);

            return !claimsJws.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Expired or invalid JWT token");
        }
    }

    public String getUsername(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public TokenType getTokenType(String token) {
        String typeString = getClaimFromToken(token, claims -> claims.get("token_type", String.class));
        if (typeString == null) {
            throw new JwtException("Token type claim is missing");
        }
        return TokenType.valueOf(typeString);
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claimsResolver.apply(claims);
    }
}
