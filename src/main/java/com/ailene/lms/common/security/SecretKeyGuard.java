package com.ailene.lms.common.security;

import com.ailene.lms.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretKeyGuard {

    private final String secretKey;

    public SecretKeyGuard(@Value("${security.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public void requireValidSecretKey(String authorization) {
        if (!extractBearerToken(authorization).equals(secretKey)) {
            throw new UnauthorizedException("Bearer token is invalid");
        }
    }

    public String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid authorization header");
        }
        return authorization.substring("Bearer ".length());
    }
}
