package com.taskflow.taskservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Wire format for tasks — includes file attachment info.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDto {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be <= 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must be <= 1000 characters")
    private String description;

    private String status;
    private String assignee;
    private LocalDateTime assignedTime;
    private LocalDateTime completedTime;

    /** Server-computed: whether the current caller may update this task. */
    private Boolean canEdit;

    /** Original filename of file attached by admin (shown to user). */
    private String assignedFileOriginalName;

    /** Original filename of file uploaded by user when completing. */
    private String completedFileOriginalName;

    /** Whether admin has attached a file to this task. */
    private Boolean hasAssignedFile;

    /** Whether user has uploaded a completed file. */
    private Boolean hasCompletedFile;

    public TaskDto() {}

    public TaskDto(Long id, String title, String description, String status, String assignee,
                   LocalDateTime assignedTime, LocalDateTime completedTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignee = assignee;
        this.assignedTime = assignedTime;
        this.completedTime = completedTime;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public LocalDateTime getAssignedTime() { return assignedTime; }
    public void setAssignedTime(LocalDateTime assignedTime) { this.assignedTime = assignedTime; }

    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }

    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }

    public String getAssignedFileOriginalName() { return assignedFileOriginalName; }
    public void setAssignedFileOriginalName(String assignedFileOriginalName) { this.assignedFileOriginalName = assignedFileOriginalName; }

    public String getCompletedFileOriginalName() { return completedFileOriginalName; }
    public void setCompletedFileOriginalName(String completedFileOriginalName) { this.completedFileOriginalName = completedFileOriginalName; }

    public Boolean getHasAssignedFile() { return hasAssignedFile; }
    public void setHasAssignedFile(Boolean hasAssignedFile) { this.hasAssignedFile = hasAssignedFile; }

    public Boolean getHasCompletedFile() { return hasCompletedFile; }
    public void setHasCompletedFile(Boolean hasCompletedFile) { this.hasCompletedFile = hasCompletedFile; }
}
