package com.dornaz.taskflowbackend.controller;

import com.dornaz.taskflowbackend.dto.common.PagedResponse;
import com.dornaz.taskflowbackend.dto.project.ProjectRequest;
import com.dornaz.taskflowbackend.dto.project.ProjectResponse;
import com.dornaz.taskflowbackend.model.Project;
import com.dornaz.taskflowbackend.model.ProjectStatus;
import com.dornaz.taskflowbackend.model.User;
import com.dornaz.taskflowbackend.repository.ProjectRepository;
import com.dornaz.taskflowbackend.repository.UserRepository;
import com.dornaz.taskflowbackend.security.CustomUserDetails;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectController(ProjectRepository projectRepository,
                             UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // ---------- helpers ----------

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails cud)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid auth principal");
        }

        return userRepository.findByEmail(cud.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found for email: " + cud.getEmail()
                ));
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse dto = new ProjectResponse();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setStatus(project.getStatus() != null ? project.getStatus().name() : null);
        dto.setDueDate(project.getDueDate());

        if (project.getOwner() != null) {
            dto.setOwnerId(project.getOwner().getId());
            dto.setOwnerEmail(project.getOwner().getEmail());
        }

        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        return dto;
    }

    private void applyRequestToProject(ProjectRequest request, Project project) {
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setDueDate(request.getDueDate());

        if (request.getStatus() != null) {
            try {
                project.setStatus(ProjectStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid status: " + request.getStatus()
                );
            }
        }
    }

    // ---------- endpoints ----------

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody ProjectRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Project project = new Project();
        applyRequestToProject(request, project);
        project.setOwner(currentUser);

        Project saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    // ✅ UPDATED: pagination + sorting (like your TaskController)
    @GetMapping
    public ResponseEntity<PagedResponse<ProjectResponse>> getMyProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(direction);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be asc or desc");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(dir, sortBy)
        );

        Page<Project> projectPage = projectRepository.findByOwner(currentUser, pageable);

        List<ProjectResponse> content = projectPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        PagedResponse<ProjectResponse> response = new PagedResponse<>(
                content,
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.getTotalElements(),
                projectPage.getTotalPages(),
                projectPage.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Project project = projectRepository.findByIdAndOwner(id, currentUser)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));

        return ResponseEntity.ok(toResponse(project));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Project project = projectRepository.findByIdAndOwner(id, currentUser)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));

        applyRequestToProject(request, project);
        Project updated = projectRepository.save(project);

        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Project project = projectRepository.findByIdAndOwner(id, currentUser)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));

        projectRepository.delete(project);
        return ResponseEntity.noContent().build();
    }
}
