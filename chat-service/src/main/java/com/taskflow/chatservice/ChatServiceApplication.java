package com.taskflow.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Chat Service — handles task-specific messaging between users and admins.
 * Runs on port 8085.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
