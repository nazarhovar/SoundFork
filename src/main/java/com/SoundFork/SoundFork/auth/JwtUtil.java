package com.SoundFork.SoundFork.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final String secret;
    private final long expirationMs = 86400000L;
    private final ObjectMapper objectMapper;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
        this.objectMapper = new ObjectMapper();
    }

    public String generateToken(String username, String role) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("role", role);
        payload.put("iat", System.currentTimeMillis() / 1000);
        payload.put("exp", (System.currentTimeMillis() + expirationMs) / 1000);

        String headerBase64 = base64UrlEncode(toJson(header));
        String payloadBase64 = base64UrlEncode(toJson(payload));
        String signature = hmacSha256(headerBase64 + "." + payloadBase64, secret);

        return headerBase64 + "." + payloadBase64 + "." + signature;
    }

    public String extractUsername(String token) {
        return extractClaim(token, "sub");
    }

    public String extractRole(String token) {
        return extractClaim(token, "role");
    }

    public boolean validateToken(String token, String username) {
        try {
            if (!verifySignature(token)) return false;
            String extractedUsername = extractUsername(token);
            if (!extractedUsername.equals(username)) return false;
            long exp = Long.parseLong(extractClaim(token, "exp"));
            return (exp * 1000) >= System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    private String extractClaim(String token, String claimName) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new RuntimeException("Invalid JWT format: expected 3 parts, got " + parts.length);
        }
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = parseJson(payloadJson);
        if (!claims.containsKey(claimName)) {
            throw new RuntimeException("Claim " + claimName + " not found in JWT");
        }
        Object value = claims.get(claimName);
        return value != null ? value.toString() : null;
    }

    private boolean verifySignature(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;
        String expectedSignature = hmacSha256(parts[0] + "." + parts[1], secret);
        return expectedSignature.equals(parts[2]);
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 error: " + e.getMessage(), e);
        }
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes(StandardCharsets.UTF_8));
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization error: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
        }
    }
}
