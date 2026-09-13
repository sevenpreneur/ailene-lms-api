package com.ailene.lms.sponsor;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SponsorAccessGuard {

    private final AuthService authService;
    private final AccessRepository accessRepository;

    public Access requireSponsor(String jwt, String projectId) {
        UUID userId = authService.resolveUserId(jwt);
        Access access = accessRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        if (access.getRole() != AccessRole.sponsor) {
            throw new ForbiddenException("Only sponsors can access this resource");
        }

        return access;
    }
}
