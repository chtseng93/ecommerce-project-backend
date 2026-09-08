package com.example.ecommerceproject.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.ecommerceproject.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtParser jwtParser;
    private final byte[] jwtKey;
    private final long expirationMinutes;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-minutes:60}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret 必須 >= 32 字元（HS256 需要 256-bit 金鑰）");
        }
        this.jwtKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
        this.jwtParser = Jwts.parser().setSigningKey(jwtKey);
    }

    public String getEmail(Claims claims) {
        return claims.getSubject();
    }

    public String createToken(User user) {
        Claims claims = Jwts.claims().setSubject(user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60_000L))
                .signWith(SignatureAlgorithm.HS256, jwtKey)
                .compact();
    }

    private Claims parseJwtClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }
    
    public boolean isValid(Claims claims) {
        return claims.getExpiration().after(new Date());
    }

    public Claims resolveClaims(HttpServletRequest httpServletRequest) {
        try {
            String token = resolveToken(httpServletRequest);
            if (token != null) {
                return parseJwtClaims(token);
            }
            return null;
        } catch (ExpiredJwtException exception) {
        	httpServletRequest.setAttribute("expired", exception.getMessage());
            throw exception;
        } catch (Exception exception) {
        	httpServletRequest.setAttribute("invalid", exception.getMessage());
            throw exception;
        }
    }

    public String resolveToken(HttpServletRequest httpServletRequest) {

        String bearerToken = httpServletRequest.getHeader(TOKEN_HEADER);
        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    

  


}
