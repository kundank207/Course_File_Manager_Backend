package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Entity.ChatMessage;
import com.mitmeerut.CFM_Portal.Service.ChatService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{chatId}/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable("chatId") String chatId,
            @RequestBody ChatMessage message,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        message.setSenderId(userDetails.getUserId());
        message.setSenderName(userDetails.getFullName());
        message.setSenderRole(userDetails.getRole().name());
        message.setSenderProfileImageUrl(userDetails.getProfileImageUrl());

        if ("department".equalsIgnoreCase(chatId)) {
            message.setDepartmentId(userDetails.getDepartmentId());
            message.setIsAdminHelp(false);
        } else {
            try {
                Long receiverId = Long.parseLong(chatId);
                message.setReceiverId(receiverId);
                message.setIsAdminHelp(true); // Assuming numeric ID means Admin Help or Private
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid chatId"));
            }
        }

        ChatMessage saved = chatService.sendMessage(message);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", saved);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable("chatId") String chatId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ChatMessage> messages;
        if ("department".equalsIgnoreCase(chatId)) {
            messages = chatService.getDepartmentMessages(userDetails.getDepartmentId(), userDetails.getUserId());
        } else {
            try {
                Long userId = Long.parseLong(chatId);
                messages = chatService.getAdminHelpHistory(userId, userDetails.getUserId());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid chatId"));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messages);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, Object>> deleteMessage(
            @PathVariable("messageId") Long messageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        chatService.deleteMessageForMe(messageId, userDetails.getUserId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seen/{senderId}")
    public ResponseEntity<Map<String, Object>> markAsSeen(
            @PathVariable("senderId") Long senderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        chatService.markAsSeen(userDetails.getUserId(), senderId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
