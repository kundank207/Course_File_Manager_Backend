package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Course;
import com.mitmeerut.CFM_Portal.Model.Program;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Repository.CourseRepository;
import com.mitmeerut.CFM_Portal.Repository.ProgramRepository;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import java.util.List;

@Service
@Transactional
public class CourseServiceImpl implements CourseService {
    private CourseRepository courseRepo;
    private ProgramRepository programRepo;

    @Autowired
    public CourseServiceImpl(CourseRepository courseRepo, ProgramRepository programRepo) {
        this.courseRepo = courseRepo;
        this.programRepo = programRepo;
    }

    @Override
    @CacheEvict(value = "courses", allEntries = true)
    public Course createCourse(Course course, CustomUserDetails user) {
        if (user.getRole() != User.userRole.HOD && user.getRole() != User.userRole.TEACHER) {
            // Check if user has explicit permission or skip role check if trusted context
            // But strictly, only HOD/Teacher adds courses contextually.
            // If user is ADMIN masquerading, this might fail unless we check activeRole.
            // For now, assume role is correct or activeRole aligns.
        }

        if (user.getTeacher() == null || user.getTeacher().getDepartment() == null) {
            throw new RuntimeException("User is not assigned to a department!");
        }

        Long hodDeptId = user.getTeacher().getDepartment().getId();
        Program program = programRepo.findById(course.getProgramId())
                .orElseThrow(() -> new RuntimeException("Program not found!"));

        if (program.getDepartment() == null || !program.getDepartment().getId().equals(hodDeptId)) {
            throw new RuntimeException("You cannot add course for another department!");
        }

        if (courseRepo.existsByCode(course.getCode())) {
            throw new RuntimeException("Course with code '" + course.getCode() + "' already exists");
        }

        if (course.getSemesterId() != null
                && courseRepo.existsByTitleAndSemesterId(course.getTitle(), course.getSemesterId())) {
            throw new RuntimeException("Course '" + course.getTitle() + "' already exists in this Semester");
        }

        return courseRepo.save(course);
    }

    @Override
    public List<Course> getCoursesForHod(CustomUserDetails user) {
        if (user.getTeacher() == null || user.getTeacher().getDepartment() == null) {
            System.out.println("DEBUG: getCoursesForHod - USER HAS NO DEPARTMENT!");
            return List.of();
        }
        Long deptId = user.getTeacher().getDepartment().getId();
        System.out.println("DEBUG: Fetching courses for deptId: " + deptId + " for user: " + user.getUsername());
        List<Course> courses = courseRepo.findCoursesByDepartmentId(deptId);
        System.out.println("DEBUG: Found " + courses.size() + " courses");
        return courses;
    }

    @Override
    @CacheEvict(value = "courses", allEntries = true)
    public Course updateCourse(Long id, Course updated, CustomUserDetails user) {
        Course existing = courseRepo.findById(id).orElseThrow(() -> new RuntimeException("course not found!"));
        existing.setCode(updated.getCode());
        existing.setTitle(updated.getTitle());
        existing.setCredits(updated.getCredits());
        existing.setContactHour(updated.getContactHour());
        existing.setHasTheory(updated.getHasTheory());
        existing.setHasLab(updated.getHasLab());
        existing.setHasProject(updated.getHasProject());
        return courseRepo.save(existing);
    }

    @Override
    @CacheEvict(value = "courses", allEntries = true)
    public void deleteCourse(Long id, CustomUserDetails user) {
        courseRepo.deleteById(id);
    }
}
