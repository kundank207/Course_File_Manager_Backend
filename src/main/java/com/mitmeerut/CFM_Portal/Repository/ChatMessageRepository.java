package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("SELECT m FROM ChatMessage m WHERE ((m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1)) AND (m.deletedFor IS EMPTY OR :currentUser NOT MEMBER OF m.deletedFor) ORDER BY m.timestamp ASC")
    List<ChatMessage> findMessagesBetween(@Param("user1") Long user1, @Param("user2") Long user2,
            @Param("currentUser") Long currentUser);

    @Query("SELECT m FROM ChatMessage m WHERE m.departmentId = :deptId AND m.isAdminHelp = false AND (m.deletedFor IS EMPTY OR :currentUser NOT MEMBER OF m.deletedFor) ORDER BY m.timestamp ASC")
    List<ChatMessage> findByDepartmentId(@Param("deptId") Long deptId, @Param("currentUser") Long currentUser);

    @Query("SELECT m FROM ChatMessage m WHERE m.isAdminHelp = true " +
            "AND ((m.senderId = :userId AND m.receiverId = :currentUserId) OR (m.senderId = :currentUserId AND m.receiverId = :userId)) "
            +
            "AND (m.deletedFor IS EMPTY OR :currentUserId NOT MEMBER OF m.deletedFor) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findAdminHelpHistory(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId);

    @Query("SELECT m FROM ChatMessage m WHERE m.isAdminHelp = true " +
            "AND (m.senderId = :userId OR m.receiverId = :userId) " +
            "AND (m.deletedFor IS EMPTY OR :adminUserId NOT MEMBER OF m.deletedFor) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findAdminAuditHistory(@Param("userId") Long userId, @Param("adminUserId") Long adminUserId);
}
