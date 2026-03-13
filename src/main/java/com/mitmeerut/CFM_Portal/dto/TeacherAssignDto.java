package com.mitmeerut.CFM_Portal.dto;
import jakarta.validation.constraints.NotNull;

public class TeacherAssignDto {
    private Long id;
    @NotNull private Long courseId;
    @NotNull private Long teacherId;
    private String section;
    private String academicYear;
    // getters/setters
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getCourseId(){return courseId;} public void setCourseId(Long courseId){this.courseId=courseId;}
    public Long getTeacherId(){return teacherId;} public void setTeacherId(Long teacherId){this.teacherId=teacherId;}
    public String getSection(){return section;} public void setSection(String section){this.section=section;}
    public String getAcademicYear(){return academicYear;} public void setAcademicYear(String academicYear){this.academicYear=academicYear;}
}

