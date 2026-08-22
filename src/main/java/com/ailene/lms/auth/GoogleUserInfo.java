package com.ailene.lms.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record GoogleUserInfo(String sub, String email, @JsonProperty("email_verified") Boolean emailVerified, String name,
        String picture) {
}
