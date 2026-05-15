package com.wha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wha.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class GoogleOAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record GoogleUserInfo(String email, String firstName, String lastName, String googleId) {}

    public GoogleUserInfo verifyIdToken(String idToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw AppException.unauthorized("Invalid Google token");
            }

            Map<?, ?> payload = MAPPER.readValue(response.body(), Map.class);

            if (payload.containsKey("error_description")) {
                throw AppException.unauthorized("Invalid Google token");
            }

            String aud = (String) payload.get("aud");
            if (!clientId.equals(aud)) {
                throw AppException.unauthorized("Token audience mismatch");
            }

            Object verified = payload.get("email_verified");
            boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equals(String.valueOf(verified));
            if (!emailVerified) {
                throw AppException.badRequest("Google account email is not verified");
            }

            return new GoogleUserInfo(
                    (String) payload.get("email"),
                    (String) payload.get("given_name"),
                    (String) payload.get("family_name"),
                    (String) payload.get("sub")
            );
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw AppException.unauthorized("Could not verify Google token: " + e.getMessage());
        }
    }
}
