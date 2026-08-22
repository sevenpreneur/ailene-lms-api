package com.ailene.lms.auth;

import com.ailene.lms.common.exception.UnauthorizedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleTokenVerifier {

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
        } catch (RestClientException e) {
            throw new UnauthorizedException("Failed to verify Google access token");
        }

        if (userInfo == null || !Boolean.TRUE.equals(userInfo.emailVerified())) {
            throw new UnauthorizedException("Invalid or unverified Google access token");
        }

        return userInfo;
    }
}
