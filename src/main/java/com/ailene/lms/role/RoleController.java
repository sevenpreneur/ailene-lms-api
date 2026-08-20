package com.ailene.lms.role;

import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
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
    public List<RoleDto> findAll() {
        return roleRepository.findAll().stream().map(RoleDto::from).toList();
    }

    @GetMapping("/{id}")
    public RoleDto findById(@PathVariable Short id) {
        return roleRepository.findById(id)
                .map(RoleDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Role %d not found".formatted(id)));
    }
}
