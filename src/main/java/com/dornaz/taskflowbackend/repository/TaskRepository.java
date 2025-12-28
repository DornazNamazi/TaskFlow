package com.dornaz.taskflowbackend.repository;

import com.dornaz.taskflowbackend.model.Project;
import com.dornaz.taskflowbackend.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // ✅ pagination + sorting support
    Page<Task> findByProject(Project project, Pageable pageable);

    Optional<Task> findByIdAndProject(Long id, Project project);

    void deleteByIdAndProject(Long id, Project project);
}
