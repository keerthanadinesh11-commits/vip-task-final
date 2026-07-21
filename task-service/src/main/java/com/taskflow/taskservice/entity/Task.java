package com.taskflow.taskservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Task entity — includes file attachment support.
 * Admin can attach a file when assigning; user uploads completed file.
 */
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 30)
    private String status;

    @Column(length = 100)
    private String assignee;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime assignedTime;

    private LocalDateTime completedTime;

    @Column(length = 500)
    private String assignedFileName;

    @Column(length = 500)
    private String assignedFileOriginalName;

    @Column(length = 500)
    private String completedFileName;

    @Column(length = 500)
    private String completedFileOriginalName;

    public Task() {}

    public Task(Long id, String title, String description, String status, String assignee) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignee = assignee;
    }

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public String getTitle()                    { return title; }
    public void setTitle(String title)          { this.title = title; }

    public String getDescription()              { return description; }
    public void setDescription(String desc)     { this.description = desc; }

    public String getStatus()                   { return status; }
    public void setStatus(String status)        { this.status = status; }

    public String getAssignee()                 { return assignee; }
    public void setAssignee(String assignee)    { this.assignee = assignee; }

    public LocalDateTime getAssignedTime()      { return assignedTime; }
    public void setAssignedTime(LocalDateTime t){ this.assignedTime = t; }

    public LocalDateTime getCompletedTime()      { return completedTime; }
    public void setCompletedTime(LocalDateTime t){ this.completedTime = t; }

    public String getAssignedFileName()          { return assignedFileName; }
    public void setAssignedFileName(String n)    { this.assignedFileName = n; }

    public String getAssignedFileOriginalName()        { return assignedFileOriginalName; }
    public void setAssignedFileOriginalName(String n)  { this.assignedFileOriginalName = n; }

    public String getCompletedFileName()               { return completedFileName; }
    public void setCompletedFileName(String n)         { this.completedFileName = n; }

    public String getCompletedFileOriginalName()       { return completedFileOriginalName; }
    public void setCompletedFileOriginalName(String n) { this.completedFileOriginalName = n; }
}
