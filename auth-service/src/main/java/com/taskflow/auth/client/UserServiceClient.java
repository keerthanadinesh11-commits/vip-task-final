package com.taskflow.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @PostMapping("/users/internal/create")
    void createUserProfile(@RequestBody Map<String, String> userProfile);

    @PostMapping("/users/internal/record-login")
    void recordLogin(@RequestBody Map<String, String> body);

    @PostMapping("/users/internal/update-status")
    void updateStatus(@RequestBody Map<String, String> body);
}
