package com.wha.service;

import com.wha.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleOAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    public record GoogleUserInfo(String email, String firstName, String lastName, String googleId) {}

    public GoogleUserInfo verifyIdToken(String idToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        RestTemplate rest = new RestTemplate(factory);
        try {
            Map<?, ?> payload = rest.getForObject(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                    Map.class);

            if (payload == null || payload.containsKey("error_description")) {
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
        } catch (RestClientException e) {
            throw AppException.unauthorized("Could not verify Google token: " + e.getMessage());
        }
    }
}
