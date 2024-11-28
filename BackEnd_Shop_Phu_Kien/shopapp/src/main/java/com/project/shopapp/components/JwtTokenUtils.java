package com.project.shopapp.components;

import com.project.shopapp.exceptions.InvalidParamException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtTokenUtils {

    @Value("${jwt.expiration}")
    private int expiration; // Store securely in environment variables

    @Value("${jwt.secretKey}")
    private String secretKey; // Securely store in environment variables

    /**
     * Generates a JWT token for a given user.
     * 
     * @param user The user object containing required claims.
     * @return A JWT token as a string.
     * @throws InvalidParamException if the token cannot be created.
     */
    public String generateToken(com.project.shopapp.models.User user) {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new InvalidParamException("Secret key is not configured.");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("phoneNumber", user.getPhoneNumber());
        claims.put("userId", user.getId());

        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(user.getPhoneNumber())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L))
                    .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            throw new InvalidParamException("Cannot create JWT token: " + e.getMessage());
        }
    }

    /**
     * Retrieves the signing key from the secret key.
     * 
     * @return The signing key.
     */
    private Key getSignInKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new InvalidParamException("Invalid secret key format.");
        }
    }

    /**
     * Generates a new secret key for HMAC signing.
     * 
     * @return A base64-encoded secret key.
     */
    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256-bit key
        random.nextBytes(keyBytes);
        return Encoders.BASE64.encode(keyBytes);
    }

    /**
     * Extracts all claims from a JWT token.
     * 
     * @param token The JWT token.
     * @return The claims extracted from the token.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new InvalidParamException("Failed to extract claims: " + e.getMessage());
        }
    }

    /**
     * Extracts a specific claim using a resolver function.
     * 
     * @param token The JWT token.
     * @param claimsResolver A function to resolve the desired claim.
     * @param <T> The type of the resolved claim.
     * @return The resolved claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Checks if a token is expired.
     * 
     * @param token The JWT token.
     * @return True if the token is expired, false otherwise.
     */
    public boolean isTokenExpired(String token) {
        Date expirationDate = extractClaim(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }

    /**
     * Extracts the phone number (subject) from a token.
     * 
     * @param token The JWT token.
     * @return The phone number.
     */
    public String extractPhoneNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates a token against the provided user details.
     * 
     * @param token The JWT token.
     * @param userDetails The user details to validate against.
     * @return True if the token is valid, false otherwise.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        String phoneNumber = extractPhoneNumber(token);
        return (phoneNumber.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
