package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_file", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "section", "course_id", "academicYear", "revision_number" })
}, indexes = {
        @Index(name = "idx_coursefile_status", columnList = "status"),
        @Index(name = "idx_coursefile_createdby", columnList = "created_by")
})
@Data
public class CourseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "template_id")
    private Template template;

    private String academicYear;

    private String section;

    @Column(name = "revision_number")
    private Integer revisionNumber = 1;

    @Column(name = "parent_file_id")
    private Long parentFileId;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Teacher createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    private String rejectedByRole;

    private LocalDateTime rejectedAt;

    @Lob
    private String systemComment;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
