package com.ailene.lms.admin;

import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AdminGuard {

    private final AdminRepository adminRepository;
    private final ObjectMapper objectMapper;

    // The identity service signs this token with its own key, so an exact active tokens row is what proves it.
    public AdminTokenOwnerProjection requireAdmin(String token) {
        AdminTokenOwnerProjection owner = adminRepository.findActiveTokenOwner(token)
                .orElseThrow(() -> new UnauthorizedException("Session not found or already ended"));
        if (isExpired(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }
        if (!"administrator".equals(owner.getRole()) || !"active".equals(owner.getStatus())
                || Boolean.TRUE.equals(owner.getDeleted())) {
            throw new ForbiddenException("Only administrators can access this resource");
        }
        return owner;
    }

    public void requireAdminOnProject(String token, String projectId) {
        requireAdmin(token);
        if (!adminRepository.projectExists(projectId)) {
            throw new ResourceNotFoundException("Project not found");
        }
    }

    private boolean isExpired(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return true;
        }
        try {
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode exp = claims.get("exp");
            return exp == null || !exp.isNumber() || Instant.ofEpochSecond(exp.asLong()).isBefore(Instant.now());
        } catch (IllegalArgumentException | JacksonException e) {
            return true;
        }
    }
}
