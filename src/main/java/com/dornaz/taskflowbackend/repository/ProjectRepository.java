package com.dornaz.taskflowbackend.repository;

import com.dornaz.taskflowbackend.model.Project;
import com.dornaz.taskflowbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwner(User owner);

    Optional<Project> findByIdAndOwner(Long id, User owner);
}
