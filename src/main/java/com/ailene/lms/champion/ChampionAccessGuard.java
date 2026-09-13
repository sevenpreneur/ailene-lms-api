package com.ailene.lms.champion;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.access.GroupSummaryProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.exception.BadRequestException;
import com.ailene.lms.common.exception.ForbiddenException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChampionAccessGuard {

    private final AuthService authService;
    private final AccessRepository accessRepository;

    public ChampionContext requireChampion(String jwt, String projectId) {
        UUID userId = authService.resolveUserId(jwt);
        Access access = accessRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));

        if (access.getRole() != AccessRole.champion) {
            throw new ForbiddenException("Only champions can access this resource");
        }

        GroupSummaryProjection summary = accessRepository.findGroupSummary(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
        if (summary.getGroupId() == null) {
            throw new BadRequestException("You don't lead a group in this project yet");
        }

        return new ChampionContext(access.getId(), projectId, summary.getGroupId(), summary.getGroupName());
    }
}
