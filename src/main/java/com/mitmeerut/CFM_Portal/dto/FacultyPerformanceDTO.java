package com.mitmeerut.CFM_Portal.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FacultyPerformanceDTO {
    private Long id;
    private String name;
    private String designation;
    private String email;
    private Integer totalCourses;
    private Long totalUploads;
    private Double approvalRate;
    private LocalDateTime lastActivity;
    private Double engagementScore;

    public FacultyPerformanceDTO(Long id, String name, String designation, String email,
            Integer totalCourses, Long totalUploads, Double approvalRate,
            LocalDateTime lastActivity, Double engagementScore) {
        this.id = id;
        this.name = name;
        this.designation = designation;
        this.email = email;
        this.totalCourses = totalCourses;
        this.totalUploads = totalUploads;
        this.approvalRate = approvalRate;
        this.lastActivity = lastActivity;
        this.engagementScore = engagementScore;
    }
}
