package com.ailene.lms.student;

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
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final SecretKeyGuard secretKeyGuard;

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<StudentStatusResponse>> status(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody StudentStatusRequest request) {
        String jwt = secretKeyGuard.extractBearerToken(authorization);
        StudentStatusResponse response = studentService.getStatus(jwt, request);
        return ApiResponse.success(HttpStatus.OK, "student status retrieved successfully", response);
    }
}
