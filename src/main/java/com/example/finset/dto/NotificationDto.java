package com.example.finset.dto;

import com.example.finset.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class NotificationDto {

    @Data
    public static class Response {
        private UUID          id;
        private Notification.Type type;
        private String        title;
        private String        body;
        private String        actionUrl;
        private boolean       read;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ListResponse {
        private List<Response> notifications;
        private long           unreadCount;
    }
}