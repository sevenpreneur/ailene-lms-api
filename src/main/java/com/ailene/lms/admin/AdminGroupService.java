package com.ailene.lms.admin;

import com.ailene.lms.common.exception.ConflictException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import com.ailene.lms.group.ProjectGroup;
import com.ailene.lms.group.ProjectGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminGroupService {

    private final AdminGuard adminGuard;
    private final AdminRepository adminRepository;
    private final ProjectGroupRepository projectGroupRepository;

    public List<AdminProjectDto> listProjects(String token) {
        adminGuard.requireAdmin(token);
        return adminRepository.findProjects().stream().map(AdminProjectDto::from).toList();
    }

    public List<AdminGroupDto> listGroups(String token, AdminProjectRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        return projectGroupRepository.findGroupList(request.projectId()).stream().map(AdminGroupDto::from).toList();
    }

    public AdminGroupDto createGroup(String token, CreateGroupRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        String name = request.name().trim();
        if (projectGroupRepository.existsByProjectIdAndNameIgnoreCase(request.projectId(), name)) {
            throw new ConflictException("A group with this name already exists in this project");
        }

        ProjectGroup group = new ProjectGroup();
        group.setProjectId(request.projectId());
        group.setName(name);
        Integer groupId = projectGroupRepository.save(group).getId();
        return findGroup(request.projectId(), groupId);
    }

    public AdminGroupDto updateGroup(String token, UpdateGroupRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        ProjectGroup group = requireGroup(request.projectId(), request.groupId());
        String name = request.name().trim();
        if (projectGroupRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(request.projectId(), name,
                group.getId())) {
            throw new ConflictException("A group with this name already exists in this project");
        }

        group.setName(name);
        projectGroupRepository.save(group);
        return findGroup(request.projectId(), group.getId());
    }

    // Members and sessions are checked up front for a clear message; the FK catch covers anything newer.
    public AdminDeleteResponse deleteGroup(String token, DeleteGroupRequest request) {
        adminGuard.requireAdminOnProject(token, request.projectId());
        ProjectGroup group = requireGroup(request.projectId(), request.groupId());
        if (projectGroupRepository.countMembers(group.getId()) > 0) {
            throw new ConflictException("This group still has members; move or remove them first");
        }
        if (projectGroupRepository.countSessions(group.getId()) > 0) {
            throw new ConflictException("This group still has chapter sessions scheduled for it");
        }

        try {
            projectGroupRepository.delete(group);
            projectGroupRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This group is still in use and cannot be deleted");
        }
        return new AdminDeleteResponse(true);
    }

    ProjectGroup requireGroup(String projectId, Integer groupId) {
        return projectGroupRepository.findByIdAndProjectId(groupId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    private AdminGroupDto findGroup(String projectId, Integer groupId) {
        return projectGroupRepository.findGroupList(projectId).stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .map(AdminGroupDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }
}
