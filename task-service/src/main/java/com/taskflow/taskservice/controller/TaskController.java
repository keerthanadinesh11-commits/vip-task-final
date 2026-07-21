package com.taskflow.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.taskservice.dto.TaskDto;
import com.taskflow.taskservice.entity.Task;
import com.taskflow.taskservice.service.FileStorageService;
import com.taskflow.taskservice.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "Task creation, assignment, updates and file attachments")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskController {

    private final TaskService service;
    private final FileStorageService fileStorageService;

    public TaskController(TaskService service, FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "List all tasks")
    public ResponseEntity<List<TaskDto>> getAll(Authentication authentication) {
        String email = authentication.getName();
        String role  = extractRole(authentication);
        List<TaskDto> tasks = service.getAll().stream()
                .map(task -> toDto(task, service.canUpdate(task, email, role)))
                .toList();
        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    @Operation(summary = "Create a task")
    public ResponseEntity<TaskDto> create(@Valid @RequestBody TaskDto dto,
                                          Authentication authentication) {
        Task saved = service.createTask(toEntity(dto));
        String role = extractRole(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(saved, service.canUpdate(saved, authentication.getName(), role)));
    }

    @PostMapping(value = "/assign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Assign a task with optional file attachment — ADMIN only")
    public ResponseEntity<TaskDto> assign(
            @RequestParam("task") String taskJson,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        TaskDto dto;
        try {
            dto = new ObjectMapper().readValue(taskJson, TaskDto.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        MultipartFile fileArg = (file != null && !file.isEmpty()) ? file : null;
        Task saved = service.assignTask(toEntity(dto), fileArg, fileStorageService);
        return ResponseEntity.ok(toDto(saved,
                service.canUpdate(saved, authentication.getName(), extractRole(authentication))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskDto> update(@PathVariable Long id,
                                          @Valid @RequestBody TaskDto dto,
                                          Authentication authentication) {
        String email = authentication.getName();
        String role  = extractRole(authentication);
        Task saved   = service.updateTask(id, toEntity(dto), email, role);
        return ResponseEntity.ok(toDto(saved, service.canUpdate(saved, email, role)));
    }

    @PostMapping(value = "/{id}/upload-completed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "User uploads completed work file for a task")
    @ApiResponse(responseCode = "200", description = "Completed file uploaded")
    @ApiResponse(responseCode = "403", description = "Not the assignee")
    public ResponseEntity<TaskDto> uploadCompletedFile(
            @PathVariable Long id,
            HttpServletRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        String role  = extractRole(authentication);

        // Extract file from multipart request manually for max gateway compatibility
        MultipartFile file = null;
        if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest) {
            file = multipartRequest.getFile("file");
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Task saved = service.uploadCompletedFile(id, file, email, role, fileStorageService);
        return ResponseEntity.ok(toDto(saved, service.canUpdate(saved, email, role)));
    }

    @GetMapping("/{id}/download-assigned")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Download the file admin attached when assigning the task")
    public ResponseEntity<Resource> downloadAssignedFile(@PathVariable Long id) {
        Task task = service.getById(id);
        if (task.getAssignedFileName() == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = fileStorageService.loadFile(task.getAssignedFileName());
        String originalName = task.getAssignedFileOriginalName() != null
                ? task.getAssignedFileOriginalName() : task.getAssignedFileName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/{id}/download-completed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','USER')")
    @Operation(summary = "Download the file user uploaded after completing the task")
    public ResponseEntity<Resource> downloadCompletedFile(@PathVariable Long id) {
        Task task = service.getById(id);
        if (task.getCompletedFileName() == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = fileStorageService.loadFile(task.getCompletedFileName());
        String originalName = task.getCompletedFileOriginalName() != null
                ? task.getCompletedFileOriginalName() : task.getCompletedFileName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}/completed-file")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Admin deletes the completed file uploaded by user")
    public ResponseEntity<TaskDto> deleteCompletedFile(@PathVariable Long id,
                                                        Authentication authentication) {
        String email = authentication.getName();
        String role  = extractRole(authentication);
        Task saved   = service.deleteCompletedFile(id, fileStorageService);
        return ResponseEntity.ok(toDto(saved, service.canUpdate(saved, email, role)));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
    }

    private static Task toEntity(TaskDto dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setAssignee(dto.getAssignee());
        return task;
    }

    private static TaskDto toDto(Task task, boolean canEdit) {
        TaskDto dto = new TaskDto(
                task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getAssignee(),
                task.getAssignedTime(), task.getCompletedTime());
        dto.setCanEdit(canEdit);
        dto.setAssignedFileOriginalName(task.getAssignedFileOriginalName());
        dto.setCompletedFileOriginalName(task.getCompletedFileOriginalName());
        dto.setHasAssignedFile(task.getAssignedFileName() != null);
        dto.setHasCompletedFile(task.getCompletedFileName() != null);
        return dto;
    }
}
