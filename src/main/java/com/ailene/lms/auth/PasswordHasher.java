package com.ailene.lms.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    // Checked against when the email is unknown, so a miss costs the same time as a wrong password.
    private final String decoyHash = encoder.encode("decoy-password-for-timing");

    public String hash(String password) {
        return encoder.encode(password);
    }

    public boolean matches(String password, String hash) {
        if (hash == null || hash.isBlank()) {
            encoder.matches(password, decoyHash);
            return false;
        }
        try {
            return encoder.matches(password, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
