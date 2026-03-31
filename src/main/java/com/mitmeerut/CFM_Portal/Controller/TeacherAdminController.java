
package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Service.TeacherService;
import com.mitmeerut.CFM_Portal.dto.FacultyResponse;
import com.mitmeerut.CFM_Portal.dto.FacultyUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/faculty")

@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_SYSTEM')")
public class TeacherAdminController {

    @Autowired
    private TeacherService teacherService;

    @GetMapping("/all")
    public ResponseEntity<List<FacultyResponse>> getAllFaculty() {
        return ResponseEntity.ok(teacherService.getFacultyList());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getFacultyDetails(@PathVariable("userId") Long userId) {

        Teacher teacher = teacherService.getTeacherByUserId(userId);
        return ResponseEntity.ok(teacher);
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<?> updateFaculty(
            @PathVariable("userId") Long userId,
            @RequestBody FacultyUpdateRequest dto) {

        User updated = teacherService.updateUserAndTeacher(userId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteFaculty(@PathVariable("userId") Long userId) {
        teacherService.deleteUserAndTeacher(userId);
        return ResponseEntity.ok("Faculty deleted successfully");
    }

    @GetMapping("/hod-list")
    public ResponseEntity<?> getHodList() {
        return ResponseEntity.ok(teacherService.getHodList());
    }

}
