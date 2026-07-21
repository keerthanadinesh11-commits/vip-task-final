package com.taskflow.taskservice.client;

import com.taskflow.taskservice.dto.NotificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications/internal")
    void sendNotification(@RequestBody NotificationDto notification);
}
