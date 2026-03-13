package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Entity.ChatMessage;
import java.util.List;

public interface ChatService {
    ChatMessage sendMessage(ChatMessage message);

    List<ChatMessage> getChatHistory(Long user1Id, Long user2Id, Long currentUserId);

    List<ChatMessage> getDepartmentMessages(Long departmentId, Long currentUserId);

    List<ChatMessage> getAdminHelpHistory(Long userId, Long currentUserId);

    void markAsSeen(Long receiverId, Long senderId);

    void deleteMessageForMe(Long messageId, Long userId);
}
