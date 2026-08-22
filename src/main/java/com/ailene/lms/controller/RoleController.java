package com.ailene.lms.controller;

import com.ailene.lms.dto.RoleDto;
import com.ailene.lms.exception.ResourceNotFoundException;
import com.ailene.lms.repository.RoleRepository;
import com.ailene.lms.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> findAll() {
        List<RoleDto> roles = roleRepository.findAll().stream().map(RoleDto::from).toList();
        return ApiResponse.success(HttpStatus.OK, "roles retrieved successfully", roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> findById(@PathVariable Short id) {
        RoleDto role = roleRepository.findById(id)
                .map(RoleDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Role %d not found".formatted(id)));
        return ApiResponse.success(HttpStatus.OK, "role retrieved successfully", role);
    }
}
