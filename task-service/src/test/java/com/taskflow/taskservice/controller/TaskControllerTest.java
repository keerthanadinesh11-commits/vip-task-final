package com.taskflow.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.taskservice.dto.TaskDto;
import com.taskflow.taskservice.entity.Task;
import com.taskflow.taskservice.exception.ForbiddenException;
import com.taskflow.taskservice.exception.TaskNotFoundException;
import com.taskflow.taskservice.service.FileStorageService;
import com.taskflow.taskservice.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  TaskService taskService;
    @MockBean  FileStorageService fileStorageService;

    // ─── GET /tasks ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void getAll_returnsTaskList() throws Exception {
        Task t = new Task(1L, "Fix bug", "desc", "ASSIGNED", "alice@x.com");
        when(taskService.getAll()).thenReturn(List.of(t));
        when(taskService.canUpdate(any(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Fix bug"))
                .andExpect(jsonPath("$[0].canEdit").value(true));
    }

    @Test
    @WithMockUser(username = "user@x.com", roles = "USER")
    void getAll_asUser_returnsTaskListWithCanEditFalse() throws Exception {
        Task t = new Task(1L, "Task", "desc", "PENDING", "other@x.com");
        when(taskService.getAll()).thenReturn(List.of(t));
        when(taskService.canUpdate(any(), anyString(), anyString())).thenReturn(false);

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].canEdit").value(false));
    }

    @Test
    void getAll_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                result.getResponse().getStatus() == 401 ||
                                result.getResponse().getStatus() == 403));
    }

    // ─── POST /tasks ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void createTask_asAdmin_returns201() throws Exception {
        Task saved = new Task(1L, "New Task", "desc", "PENDING", "alice@x.com");
        when(taskService.createTask(any())).thenReturn(saved);
        when(taskService.canUpdate(any(), anyString(), anyString())).thenReturn(true);

        TaskDto dto = new TaskDto(null, "New Task", "desc", "PENDING", "alice@x.com", null, null);
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Task"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createTask_asUser_returns403() throws Exception {
        TaskDto dto = new TaskDto(null, "New Task", "desc", "PENDING", "alice@x.com", null, null);
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTask_blankTitle_returns400() throws Exception {
        TaskDto dto = new TaskDto(null, "", "desc", "PENDING", null, null, null);
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /tasks/assign (multipart) ───────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void assignTask_asAdmin_noFile_returns200() throws Exception {
        Task saved = new Task(1L, "Task", "desc", "ASSIGNED", "bob@x.com");
        // assignTask now takes (Task, MultipartFile, FileStorageService)
        when(taskService.assignTask(any(), isNull(), any())).thenReturn(saved);
        when(taskService.canUpdate(any(), anyString(), anyString())).thenReturn(true);

        TaskDto dto = new TaskDto(null, "Task", "desc", "PENDING", "bob@x.com", null, null);

        // Send as multipart with just the task part (no file)
        MockMultipartFile taskPart = new MockMultipartFile(
                "task", "", "application/json",
                objectMapper.writeValueAsBytes(dto));

        mockMvc.perform(multipart("/tasks/assign").file(taskPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void assignTask_asAdmin_withFile_returns200() throws Exception {
        Task saved = new Task(1L, "Task", "desc", "ASSIGNED", "bob@x.com");
        saved.setAssignedFileName("uuid-file.pdf");
        saved.setAssignedFileOriginalName("specs.pdf");
        when(taskService.assignTask(any(), any(), any())).thenReturn(saved);
        when(taskService.canUpdate(any(), anyString(), anyString())).thenReturn(true);
        when(fileStorageService.storeFile(any())).thenReturn("uuid-file.pdf");

        TaskDto dto = new TaskDto(null, "Task", "desc", "PENDING", "bob@x.com", null, null);

        MockMultipartFile taskPart = new MockMultipartFile(
                "task", "", "application/json",
                objectMapper.writeValueAsBytes(dto));
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "specs.pdf", "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/tasks/assign").file(taskPart).file(filePart))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void assignTask_asUser_returns403() throws Exception {
        TaskDto dto = new TaskDto(null, "Task", "desc", "PENDING", "bob@x.com", null, null);
        MockMultipartFile taskPart = new MockMultipartFile(
                "task", "", "application/json",
                objectMapper.writeValueAsBytes(dto));
        mockMvc.perform(multipart("/tasks/assign").file(taskPart))
                .andExpect(status().isForbidden());
    }

    // ─── PUT /tasks/{id} ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void updateTask_assignee_returns200() throws Exception {
        Task saved = new Task(1L, "Task", "desc", "IN_PROGRESS", "alice@x.com");
        when(taskService.updateTask(anyLong(), any(), anyString(), anyString())).thenReturn(saved);
        when(taskService.canUpdate(any(), anyString(), anyString())).thenReturn(true);

        TaskDto dto = new TaskDto(null, "Task", "desc", "IN_PROGRESS", "alice@x.com", null, null);
        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "bob@x.com", roles = "USER")
    void updateTask_notAssignee_returns403() throws Exception {
        when(taskService.updateTask(anyLong(), any(), anyString(), anyString()))
                .thenThrow(new ForbiddenException("Only the assignee or an administrator may update this task"));

        TaskDto dto = new TaskDto(null, "Task", "desc", "IN_PROGRESS", "alice@x.com", null, null);
        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@x.com", roles = "ADMIN")
    void updateTask_notFound_returns404() throws Exception {
        when(taskService.updateTask(anyLong(), any(), anyString(), anyString()))
                .thenThrow(new TaskNotFoundException(99L));

        TaskDto dto = new TaskDto(null, "Task", "desc", "PENDING", null, null, null);
        mockMvc.perform(put("/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ─── POST /tasks/{id}/upload-completed ────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void uploadCompletedFile_returns200() throws Exception {
        Task saved = new Task(1L, "Task", "desc", "COMPLETED", "alice@x.com");
        saved.setCompletedFileName("uuid-completed.zip");
        saved.setCompletedFileOriginalName("work.zip");
        when(taskService.uploadCompletedFile(anyLong(), any(), anyString(), anyString(), any()))
                .thenReturn(saved);
        when(taskService.canUpdate(any(), anyString(), anyString())).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile(
                "file", "work.zip", "application/zip", "zip content".getBytes());

        mockMvc.perform(multipart("/tasks/1/upload-completed").file(file))
                .andExpect(status().isOk());
    }

    // ─── GET /tasks/{id}/download-assigned ───────────────────────────────────

    @Test
    @WithMockUser(username = "alice@x.com", roles = "USER")
    void downloadAssignedFile_notFound_returns404() throws Exception {
        Task task = new Task(1L, "Task", "desc", "ASSIGNED", "alice@x.com");
        // No assigned file
        when(taskService.getById(1L)).thenReturn(task);

        mockMvc.perform(get("/tasks/1/download-assigned"))
                .andExpect(status().isNotFound());
    }
}
