package com.mitmeerut.CFM_Portal.Controller;

import java.util.List;

import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mitmeerut.CFM_Portal.Model.Course;
import com.mitmeerut.CFM_Portal.Service.CourseService;

@RestController
@RequestMapping("/api/hod/courses")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class CourseController {

	private CourseService courseService;

	@Autowired
	public CourseController(CourseService courseService) {
		this.courseService = courseService;
	}

	@GetMapping
	public List<Course> getCourses(@AuthenticationPrincipal CustomUserDetails user) {
		return courseService.getCoursesForHod(user);
	}

	@PostMapping
	@PreAuthorize("hasRole('HOD') or hasRole('TEACHER') or hasAuthority('MANAGE_COURSES')")
	public Course create(@RequestBody Course course, @AuthenticationPrincipal CustomUserDetails user) {
		return courseService.createCourse(course, user);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('HOD') or hasRole('TEACHER') or hasAuthority('MANAGE_COURSES')")
	public Course update(@PathVariable("id") Long id, @RequestBody Course course,
			@AuthenticationPrincipal CustomUserDetails user) {
		return courseService.updateCourse(id, course, user);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('HOD') or hasRole('TEACHER') or hasAuthority('MANAGE_COURSES')")
	public void delte(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails user) {
		courseService.deleteCourse(id, user);
	}

}
