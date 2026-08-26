package com.ailene.lms.common.qstash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@Component
public class QStashClient {

    private static final Logger log = LoggerFactory.getLogger(QStashClient.class);
    private static final String DEFAULT_QSTASH_URL = "https://qstash.upstash.io";

    private final RestClient restClient = RestClient.create();
    private final String qstashUrl;
    private final String qstashToken;
    private final String appBaseUrl;
    private final String secretKey;

    public QStashClient(@Value("${qstash.url}") String qstashUrl, @Value("${qstash.token}") String qstashToken,
            @Value("${app.base-url}") String appBaseUrl, @Value("${security.secret-key}") String secretKey) {
        this.qstashUrl = qstashUrl == null || qstashUrl.isBlank() ? DEFAULT_QSTASH_URL
                : qstashUrl.replaceAll("/+$", "");
        this.qstashToken = qstashToken;
        this.appBaseUrl = appBaseUrl;
        this.secretKey = secretKey;
    }

    // Callback route is SECRET_KEY-gated; forwarded via Upstash-Forward-Authorization so delivery passes SecretKeyGuard.
    public void publishDelayed(String path, Object body, long delaySeconds) {
        if (qstashToken == null || qstashToken.isBlank() || appBaseUrl == null || appBaseUrl.isBlank()) {
            log.warn("QStash not configured (qstash.token/app.base-url missing) - skipping scheduled job for {}",
                    path);
            return;
        }
        String destination = appBaseUrl.replaceAll("/+$", "") + path;
        try {
            restClient.post()
                    .uri(URI.create(qstashUrl + "/v2/publish/" + destination))
                    .header("Authorization", "Bearer " + qstashToken)
                    .header("Upstash-Delay", delaySeconds + "s")
                    .header("Upstash-Forward-Authorization", "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("QStash publish failed for {}", destination, e);
        }
    }
}
