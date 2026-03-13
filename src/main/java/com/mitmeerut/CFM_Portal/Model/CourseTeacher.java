package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "course_teacher", uniqueConstraints = @UniqueConstraint(columnNames = { "course_id", "teacher_id",
        "academic_year", "section" }))
public class CourseTeacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;
    private String section;

    @Column(name = "academic_year")
    private String academicYear;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "is_subject_head")
    private Boolean isSubjectHead = false;
}