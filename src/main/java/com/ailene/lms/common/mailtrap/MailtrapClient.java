package com.ailene.lms.common.mailtrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class MailtrapClient {

    private static final Logger log = LoggerFactory.getLogger(MailtrapClient.class);
    private static final String API_URL = "https://send.api.mailtrap.io/api/send";
    private static final Map<String, String> SENDER = Map.of("name", "Sevenpreneur", "email",
            "no-reply@sevenpreneur.com");

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiToken;

    @Autowired
    public MailtrapClient(@Value("${mailtrap.api-token:}") String apiToken) {
        this(API_URL, apiToken);
    }

    MailtrapClient(String apiUrl, String apiToken) {
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    // Returns whether Mailtrap accepted the message; never throws, so a mail outage can't undo the caller's write.
    public boolean send(String toEmail, String toName, String subject, String text, String html, String category) {
        if (apiToken == null || apiToken.isBlank()) {
            log.warn("Mailtrap not configured (MAILTRAP_API_TOKEN missing) - skipping email to {}", toEmail);
            return false;
        }

        Map<String, Object> to = toName == null || toName.isBlank() ? Map.of("email", toEmail)
                : Map.of("email", toEmail, "name", toName);
        Map<String, Object> body = Map.of("from", SENDER, "to", List.of(to),
                "subject", subject, "text", text, "html", html, "category", category);

        try {
            restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            log.error("Mailtrap rejected email to {}: {} {}", toEmail, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (RestClientException e) {
            log.error("Mailtrap call failed for email to {}", toEmail, e);
            return false;
        }
    }
}
