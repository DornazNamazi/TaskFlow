package com.dornaz.taskflowbackend.dto.project;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;
    private LocalDate dueDate;

    @NotBlank(message = "Status is required")
    private String status;       // e.g. "OPEN", "IN_PROGRESS", "DONE"

    public ProjectRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
