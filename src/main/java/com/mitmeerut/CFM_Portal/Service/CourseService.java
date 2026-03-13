package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Course;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;

import java.util.List;
import java.util.Optional;

public interface CourseService {
	Course createCourse(Course course, CustomUserDetails user);
    List<Course>getCoursesForHod(CustomUserDetails user);
    Course updateCourse(Long id, Course course, CustomUserDetails user);
    void deleteCourse(Long id, CustomUserDetails user);
	
}
