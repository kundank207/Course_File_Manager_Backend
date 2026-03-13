package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_permission")
@Data
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName; // ADMIN, HOD, TEACHER, SUBJECTHEAD

    @Column(columnDefinition = "json")
    private String permissions; // JSON array of permission strings

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    public RolePermission() {
    }

    public RolePermission(String roleName, String permissions) {
        this.roleName = roleName;
        this.permissions = permissions;
        this.updatedAt = LocalDateTime.now();
    }
}
