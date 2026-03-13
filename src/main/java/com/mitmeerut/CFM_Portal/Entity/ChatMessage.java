package com.mitmeerut.CFM_Portal.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderId;
    private Long receiverId;
    private Long departmentId; // For department-wide chat
    private Boolean isAdminHelp = false; // For admin-help chat
    private String senderName;
    private String senderRole;
    private String senderProfileImageUrl;
    private String message;
    private boolean seen = false;
    private LocalDateTime timestamp;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Long> deletedFor = new ArrayList<>();

    private String conversationId;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
