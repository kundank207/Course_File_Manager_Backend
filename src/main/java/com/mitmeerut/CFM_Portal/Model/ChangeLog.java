package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "change_logs")
@Data
@NoArgsConstructor
public class ChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_file_id")
    private CourseFile courseFile;

    @Column(name = "version_id")
    private Integer versionId; // This is the revisionNumber of the current course file

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private Teacher updatedBy;

    @Column(name = "change_type")
    private String changeType; // Added / Modified / Removed

    @Column(name = "section_name")
    private String sectionName;

    @Lob
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChangeLog(CourseFile courseFile, Integer versionId, Teacher updatedBy, String changeType, String sectionName,
            String description) {
        this.courseFile = courseFile;
        this.versionId = versionId;
        this.updatedBy = updatedBy;
        this.changeType = changeType;
        this.sectionName = sectionName;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
