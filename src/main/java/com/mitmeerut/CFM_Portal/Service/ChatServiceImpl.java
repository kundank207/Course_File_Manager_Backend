package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Entity.ChatMessage;
import com.mitmeerut.CFM_Portal.Repository.ChatMessageRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import com.mitmeerut.CFM_Portal.Model.Notification;
import com.mitmeerut.CFM_Portal.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // Added field

    @Autowired
    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) { // Added parameter
        this.chatMessageRepository = chatMessageRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate; // Initialized field
    }

    @Override
    public ChatMessage sendMessage(ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(java.time.LocalDateTime.now());
        }

        // Strict 1-to-1 Private Chat Logic
        if (message.getReceiverId() != null) {
            message.setDepartmentId(null); // Ensure no accidental broadcast

            // Set Conversation ID (SmallerId_LargerId)
            Long p1 = message.getSenderId();
            Long p2 = message.getReceiverId();
            message.setConversationId(Math.min(p1, p2) + "_" + Math.max(p1, p2));

            ChatMessage saved = chatMessageRepository.save(message);

            // Send to Receiver
            messagingTemplate.convertAndSend("/topic/chat/user/" + message.getReceiverId(), saved);
            // Send to Sender (for sync)
            messagingTemplate.convertAndSend("/topic/chat/user/" + message.getSenderId(), saved);

            // Notify receiver(s)
            userRepository.findById(message.getReceiverId()).ifPresent(receiver -> {
                Notification notif = new Notification();
                notif.setUser(receiver);
                notif.setType("CHAT_MESSAGE");
                notif.setTitle("New message from " + message.getSenderName());
                notif.setMessage(message.getMessage());
                notif.setSender(userRepository.findById(message.getSenderId()).orElse(null));
                notificationService.send(notif);
            });

            return saved;
        }

        // Department Broadcast Logic
        if (message.getDepartmentId() != null) {
            message.setConversationId("dept_" + message.getDepartmentId());
            ChatMessage saved = chatMessageRepository.save(message);
            messagingTemplate.convertAndSend("/topic/department/" + message.getDepartmentId(), saved);

            // Notify all users in department (excluding sender)
            List<User> deptUsers = userRepository.findByTeacher_DepartmentId(message.getDepartmentId());
            for (User deptUser : deptUsers) {
                if (!deptUser.getId().equals(message.getSenderId())) {
                    Notification notif = new Notification();
                    notif.setUser(deptUser);
                    notif.setType("DEPT_CHAT");
                    notif.setTitle("Dept Chat: " + message.getSenderName());
                    notif.setMessage(message.getMessage());
                    notif.setSender(userRepository.findById(message.getSenderId()).orElse(null));
                    notificationService.send(notif);
                }
            }
            return saved;
        }

        return chatMessageRepository.save(message);
    }

    @Override
    public List<ChatMessage> getChatHistory(Long user1Id, Long user2Id, Long currentUserId) {
        return chatMessageRepository.findMessagesBetween(user1Id, user2Id, currentUserId);
    }

    @Override
    public List<ChatMessage> getDepartmentMessages(Long departmentId, Long currentUserId) {
        return chatMessageRepository.findByDepartmentId(departmentId, currentUserId);
    }

    @Override
    public List<ChatMessage> getAdminHelpHistory(Long userId, Long currentUserId) {
        return chatMessageRepository.findAdminHelpHistory(userId, currentUserId);
    }

    @Override
    public void markAsSeen(Long receiverId, Long senderId) {
        // Here receiverId is the one who SEES the message (me). senderId is the other
        // person.
        // We want to fetch messages sent by senderId to receiverId.
        // findMessagesBetween fetches both directions.
        // We pass 'receiverId' (me) as currentUserId to filter out deleted messages
        // (though irrelevant for seen marking, safe to include).
        List<ChatMessage> messages = chatMessageRepository.findMessagesBetween(receiverId, senderId, receiverId);
        List<ChatMessage> unseen = messages.stream()
                .filter(m -> m.getReceiverId().equals(receiverId) && m.getSenderId().equals(senderId) && !m.isSeen())
                .collect(Collectors.toList());

        unseen.forEach(m -> m.setSeen(true));
        chatMessageRepository.saveAll(unseen);
    }

    @Override
    public void deleteMessageForMe(Long messageId, Long userId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            if (msg.getDeletedFor() == null) {
                msg.setDeletedFor(new java.util.ArrayList<>());
            }
            boolean alreadyDeleted = msg.getDeletedFor().contains(userId);
            if (!alreadyDeleted) {
                msg.getDeletedFor().add(userId);
                chatMessageRepository.save(msg);
            }
        });
    }
}
