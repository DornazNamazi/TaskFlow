package com.dornaz.taskflowbackend.controller;

import com.dornaz.taskflowbackend.dto.common.PagedResponse;
import com.dornaz.taskflowbackend.dto.task.TaskRequest;
import com.dornaz.taskflowbackend.dto.task.TaskResponse;
import com.dornaz.taskflowbackend.model.Project;
import com.dornaz.taskflowbackend.model.Task;
import com.dornaz.taskflowbackend.model.TaskStatus;
import com.dornaz.taskflowbackend.model.User;
import com.dornaz.taskflowbackend.repository.ProjectRepository;
import com.dornaz.taskflowbackend.repository.TaskRepository;
import com.dornaz.taskflowbackend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository taskRepository,
                          ProjectRepository projectRepository,
                          UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // ---------- helpers ----------

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found for email: " + email
                ));
    }

    private void ensureProjectBelongsToUser(Project project, User user) {
        if (project.getOwner() == null || project.getOwner().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Project has no owner");
        }
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your project");
        }
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse dto = new TaskResponse();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        dto.setDueDate(task.getDueDate());
        dto.setPriority(task.getPriority());

        if (task.getProject() != null) {
            dto.setProjectId(task.getProject().getId());
            dto.setProjectName(task.getProject().getName());
        }

        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        return dto;
    }

    private void applyRequestToTask(TaskRequest request, Task task) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());

        // default priority if missing
        Integer priority = request.getPriority();
        task.setPriority(priority != null ? priority : 2);

        if (request.getStatus() != null) {
            try {
                task.setStatus(TaskStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid task status: " + request.getStatus()
                );
            }
        }
    }

    // ---------- endpoints ----------

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));

        ensureProjectBelongsToUser(project, currentUser);

        Task task = new Task();
        applyRequestToTask(request, task);
        task.setProject(project);

        Task saved = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    // ✅ UPDATED: pagination + sorting
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<PagedResponse<TaskResponse>> getTasksForProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));

        ensureProjectBelongsToUser(project, currentUser);

        // ✅ direction safe
        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(direction);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be asc or desc");
        }

        // ✅ sortBy safe (prevents 500)
        String mappedSort = switch (sortBy) {
            case "createdAt" -> "createdAt";
            case "dueDate" -> "dueDate";
            case "title" -> "title";
            case "status" -> "status";      // enum field is fine
            case "priority" -> "priority";  // MUST match Task field name EXACTLY
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sortBy. Allowed: createdAt, dueDate, title, status, priority"
            );
        };

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(dir, mappedSort));

        Page<Task> taskPage = taskRepository.findByProject(project, pageable);

        List<TaskResponse> content = taskPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        PagedResponse<TaskResponse> response = new PagedResponse<>(
                content,
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.isLast()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"
                ));

        ensureProjectBelongsToUser(task.getProject(), currentUser);

        return ResponseEntity.ok(toResponse(task));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"
                ));

        ensureProjectBelongsToUser(task.getProject(), currentUser);

        applyRequestToTask(request, task);
        Task updated = taskRepository.save(task);

        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"
                ));

        ensureProjectBelongsToUser(task.getProject(), currentUser);

        taskRepository.delete(task);
        return ResponseEntity.noContent().build();
    }
}
