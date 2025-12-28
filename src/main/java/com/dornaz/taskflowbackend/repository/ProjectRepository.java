package com.dornaz.taskflowbackend.repository;

import com.dornaz.taskflowbackend.model.Project;
import com.dornaz.taskflowbackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ✅ pagination + sorting
    Page<Project> findByOwner(User owner, Pageable pageable);

    Optional<Project> findByIdAndOwner(Long id, User owner);
}
