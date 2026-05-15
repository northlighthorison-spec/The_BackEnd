package com.wha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${app.brevo.api-key}")
    private String brevoApiKey;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String link = frontendUrl + "/auth/verify?token=" + token;
        String html = buildVerificationHtml(firstName, link);

        log.info("Sending verification email to {} | link: {}", toEmail, link);
        try {
            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "Northlight Horizon", "email", fromAddress),
                "to", List.of(Map.of("email", toEmail, "name", firstName)),
                "subject", "Verify your Northlight Horizon account",
                "htmlContent", html
            );

            String jsonBody = MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", brevoApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Verification email sent to {} via Brevo (status {})", toEmail, response.statusCode());
            } else {
                log.error("Brevo rejected email to {}: status={} body={}", toEmail, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildVerificationHtml(String firstName, String link) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#f5f0e8;font-family:Georgia,serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table width="560" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:4px;overflow:hidden;">
                      <tr>
                        <td style="background:#1a2e4a;padding:32px 40px;">
                          <p style="margin:0;color:#c9a84c;font-size:11px;letter-spacing:2px;
                                    text-transform:uppercase;">Northlight Horizon</p>
                          <h1 style="margin:8px 0 0;color:#ffffff;font-size:24px;font-weight:700;">
                            Verify your email
                          </h1>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:40px;">
                          <p style="margin:0 0 16px;color:#374151;font-size:16px;">Hi %s,</p>
                          <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                            Thank you for creating an account with Northlight Horizon.
                            Please verify your email address to activate your account.
                          </p>
                          <table cellpadding="0" cellspacing="0" style="margin:0 0 24px;">
                            <tr>
                              <td style="background:#1a2e4a;border-radius:2px;">
                                <a href="%s"
                                   style="display:inline-block;padding:14px 32px;color:#ffffff;
                                          font-size:15px;font-weight:600;text-decoration:none;
                                          letter-spacing:0.3px;">
                                  Verify my email address
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:0 0 8px;color:#6b7280;font-size:13px;">
                            This link expires in 24 hours.
                          </p>
                          <p style="margin:0;color:#6b7280;font-size:13px;">
                            If you did not create this account, you can safely ignore this email.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background:#f9f7f4;padding:20px 40px;border-top:1px solid #e5e7eb;">
                          <p style="margin:0;color:#9ca3af;font-size:12px;">
                            Northlight Horizon &mdash; making a real difference, transparently.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(firstName, link);
    }
}
