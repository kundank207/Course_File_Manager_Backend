package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.CourseTeacher;
import java.util.List;
import java.util.Optional;

public interface AssignmentService {
    List<CourseTeacher> findAll();
    CourseTeacher assign(CourseTeacher ct);
    void unassign(Long id);
    Optional<CourseTeacher> findById(Long id);
    List<CourseTeacher> findByCourseId(Long courseId);
    List<CourseTeacher> findByTeacherId(Long teacherId);
}
