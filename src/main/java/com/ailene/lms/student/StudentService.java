package com.ailene.lms.student;

import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final AuthService authService;
    private final AccessRepository accessRepository;

    public StudentStatusResponse getStatus(String jwt, StudentStatusRequest request) {
        UUID userId = authService.resolveUserId(jwt);

        return accessRepository.findStudentStatus(userId, request.projectId())
                .map(StudentStatusResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No access found for this project"));
    }
}
