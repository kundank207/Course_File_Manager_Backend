package com.mitmeerut.CFM_Portal.dto;

import lombok.Data;

@Data
public class AssignCourseDTO {
    private Long courseId;
    private Long teacherId;
    private Long departmentId;
    private String section;
    private String academicYear;
    private Boolean isSubjectHead = false;
}
