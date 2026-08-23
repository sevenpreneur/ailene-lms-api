package com.ailene.lms.auth;

import com.ailene.lms.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient = RestClient.create();

    public GoogleUserInfo verify(String accessToken) {
        GoogleUserInfo userInfo;
        try {
            userInfo = restClient.get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfo.class);
        } catch (RestClientResponseException e) {
            log.warn("Google userinfo call rejected the token: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new UnauthorizedException("Failed to verify Google access token");
        } catch (RestClientException e) {
            log.error("Google userinfo call failed", e);
            throw new UnauthorizedException("Failed to verify Google access token");
        }

        if (userInfo == null || !Boolean.TRUE.equals(userInfo.emailVerified())) {
            log.warn("Google userinfo response was empty or unverified: {}", userInfo);
            throw new UnauthorizedException("Invalid or unverified Google access token");
        }

        return userInfo;
    }
}
