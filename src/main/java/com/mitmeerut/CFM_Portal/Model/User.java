package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "user", indexes = {
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_last_login", columnList = "last_login")
})
@Getter
@Setter

// matches your SQL table name
public class User {

    public enum userRole {
        ADMIN, TEACHER, HOD, SUBJECTHEAD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50)
    private userRole role;

    @OneToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "is_active")
    private Boolean isActive = false; // default: inactive (needs approval)

    @Column(name = "last_login")
    private LocalDateTime lastLogin = LocalDateTime.now();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "last_active_role", length = 50)
    private String lastActiveRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Notification> notifications = new ArrayList<>();

    // ---------------- GETTERS / SETTERS ----------------

    public String getLastActiveRole() {
        return lastActiveRole;
    }

    public void setLastActiveRole(String lastActiveRole) {
        this.lastActiveRole = lastActiveRole;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(userRole role) {
        this.role = role;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public List<String> getEffectiveRoles() {
        List<String> roles = new ArrayList<>();
        if (this.role != null) {
            String primaryRole = this.role.name();
            roles.add(primaryRole);

            // HOD and SUBJECTHEAD can also act as TEACHER
            if ("HOD".equals(primaryRole) || "SUBJECTHEAD".equals(primaryRole)) {
                roles.add("TEACHER");
            }
        }
        return roles;
    }

}
