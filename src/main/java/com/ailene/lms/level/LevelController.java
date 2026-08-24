package com.ailene.lms.level;

import com.ailene.lms.common.Status;
import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.common.security.SecretKeyGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/levels")
@RequiredArgsConstructor
public class LevelController {

    private final LevelRepository levelRepository;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<List<LevelDto>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LevelListRequest request) {
        secretKeyGuard.requireValidSecretKey(authorization);
        List<LevelDto> levels = levelRepository
                .findByProjectIdAndStatusOrderByLevelNumberAsc(request.projectId(), Status.active)
                .stream()
                .map(LevelDto::from)
                .toList();
        return ApiResponse.success(HttpStatus.OK, "levels retrieved successfully", levels);
    }
}
