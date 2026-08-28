package com.ailene.lms.category;

import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.CategorySummary;
import com.ailene.lms.common.response.ApiResponse;
import com.ailene.lms.common.security.SecretKeyGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final AuthService authService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping
    public ResponseEntity<ApiResponse<List<CategorySummary>>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        authService.resolveUserId(jwt);
        List<CategorySummary> categories = categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new CategorySummary(c.getId(), c.getName()))
                .toList();
        return ApiResponse.success(HttpStatus.OK, "categories retrieved successfully", categories);
    }
}
