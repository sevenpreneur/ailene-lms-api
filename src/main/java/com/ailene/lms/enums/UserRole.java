package com.ailene.lms.enums;

// Lowercase to match the lms_role_enum labels in Postgres exactly (Hibernate's NAMED_ENUM maps by name()).
public enum UserRole {
    student,
    champion,
    sponsor
}
