package com.ailene.lms.material;

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

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<MaterialDetailsResponse>> details(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MaterialDetailsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        MaterialDetailsResponse response = materialService.getMaterialDetails(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "material retrieved successfully", response);
    }

    @PostMapping("/completion")
    public ResponseEntity<ApiResponse<MaterialCompletionResponse>> completion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MaterialCompletionRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        MaterialCompletionResponse response = materialService.completeMaterial(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "material completed successfully", response);
    }

    @PostMapping("/in-level")
    public ResponseEntity<ApiResponse<LevelMaterialsResponse>> inLevel(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody LevelMaterialsRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        LevelMaterialsResponse response = materialService.getLevelMaterials(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "level materials retrieved successfully", response);
    }
}
