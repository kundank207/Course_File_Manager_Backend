
package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Department;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Repository.DepartmentRepository;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import com.mitmeerut.CFM_Portal.dto.FacultyResponse;
import com.mitmeerut.CFM_Portal.dto.FacultyUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {

    private TeacherRepository teacherRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private DepartmentRepository deptRepo;

    @Autowired
    public TeacherServiceImpl(TeacherRepository teacherRepository) {
        this.teacherRepo = teacherRepository;
    }

    public Teacher getTeacherByUserId(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Teacher teacher = user.getTeacher();

        if (teacher == null) {
            throw new RuntimeException("Teacher not linked with this user");
        }

        return teacher;
    }

    @Transactional
    public User updateUserAndTeacher(Long userId, FacultyUpdateRequest dto) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) Teacher fetch (linked with user)
        Teacher teacher = user.getTeacher();
        if (teacher == null) {
            throw new RuntimeException("Teacher not linked with this user");
        }

        // Handle role name variations (e.g. SUBJECT_HEAD vs SUBJECTHEAD)
        String newRoleStr = dto.getRole();
        if ("SUBJECT_HEAD".equals(newRoleStr))
            newRoleStr = "SUBJECTHEAD";

        User.userRole newRole = User.userRole.valueOf(newRoleStr);
        User.userRole oldRole = user.getRole();

        // -------------------------------------------------------------------------
        // HOD ASSIGNMENT LOGIC: Prevent 1 department having 2 HODs (Role sync)
        // -------------------------------------------------------------------------
        if (newRole == User.userRole.HOD) {
            if (dto.getDepartmentId() == null) {
                throw new RuntimeException("Department is required for HOD role");
            }

            Department targetDept = deptRepo.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            // Requirement: Check if ANY user in this department already has the HOD role
            List<User> deptUsers = userRepo.findByTeacher_DepartmentId(dto.getDepartmentId());
            for (User u : deptUsers) {
                if (u.getRole() == User.userRole.HOD && !u.getId().equals(userId)) {
                    // Show the message requested by user
                    throw new RuntimeException("already assign hod department");
                }
            }

            // Also ensure this teacher isn't HOD elsewhere (clean up previous HOD link)
            deptRepo.findByHodId(teacher.getId()).ifPresent(d -> {
                if (!d.getId().equals(targetDept.getId())) {
                    d.setHod(null);
                    deptRepo.save(d);
                }
            });

            // Set this teacher as HOD in the department entity (for two-way sync)
            targetDept.setHod(teacher);
            deptRepo.save(targetDept);

        } else if (oldRole == User.userRole.HOD) {
            // If downgrading FROM HOD, clear the department's HOD field
            deptRepo.findByHodId(teacher.getId()).ifPresent(d -> {
                d.setHod(null);
                deptRepo.save(d);
            });
        }

        // 3) Update User fields
        user.setEmail(dto.getEmail());
        user.setRole(newRole);

        // 4) Update Teacher fields
        teacher.setName(dto.getName());
        teacher.setDesignation(dto.getDesignation());
        teacher.setContactNumber(dto.getContactNumber());

        if (dto.getDepartmentId() != null) {
            Department dept = deptRepo.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            teacher.setDepartment(dept);
        } else {
            teacher.setDepartment(null);
        }

        // 5) Save both
        teacherRepo.save(teacher);
        return userRepo.save(user);
    }

    // public User updateUserAndTeacher(Long userId, FacultyUpdateRequest dto,
    // boolean force) {
    //
    // User user = userRepo.findById(userId)
    // .orElseThrow(() -> new RuntimeException("User not found"));
    //
    // Teacher teacher = user.getTeacher();
    // if (teacher == null) {
    // throw new RuntimeException("Teacher not linked with this user");
    // }
    //
    // // -----------------------------
    // // HOD CONFLICT CHECK
    // // -----------------------------
    // if (dto.getRole().equals("HOD") && dto.getDepartmentId() != null) {
    //
    // Department alreadyHodDept = deptRepo.findByHodId(userId).orElse(null);
    //
    // if (alreadyHodDept != null) {
    //
    // if (!force) {
    // throw new RuntimeException(
    // "This teacher is already HOD of " +
    // alreadyHodDept.getName() +
    // ". Do you want to assign HOD to another department also?"
    // );
    // }
    // }
    // }
    //
    // // -----------------------------
    // // IF ROLE DOWNGRADED (Remove HOD)
    // // -----------------------------
    // if (!dto.getRole().equals("HOD")) {
    //
    // Department hodOfDept = deptRepo.findByHodId(userId).orElse(null);
    //
    // if (hodOfDept != null) {
    //
    // if (!force) {
    // throw new RuntimeException(
    // "This teacher is currently HOD of " +
    // hodOfDept.getName() +
    // ". Do you want to remove HOD role?"
    // );
    // }
    //
    // hodOfDept.setHodId(null);
    // deptRepo.save(hodOfDept);
    // }
    // }

    // // -----------------------------
    // // UPDATE USER
    // // -----------------------------
    // user.setEmail(dto.getEmail());
    // user.setRole(User.userRole.valueOf(dto.getRole()));
    //
    // // -----------------------------
    // // UPDATE TEACHER
    // // -----------------------------
    // teacher.setName(dto.getName());
    // teacher.setDesignation(dto.getDesignation());
    // teacher.setContactNumber(dto.getContactNumber());
    //
    // if (dto.getDepartmentId() != null) {
    // Department dept = deptRepo.findById(dto.getDepartmentId())
    // .orElseThrow(() -> new RuntimeException("Department not found"));
    //
    // teacher.setDepartment(dept);
    //
    // // If role = HOD, assign
    // if (dto.getRole().equals("HOD")) {
    // dept.setHodId(userId);
    // deptRepo.save(dept);
    // }
    // }
    //
    // teacherRepo.save(teacher);
    // return userRepo.save(user);
    // }
    //
    //
    @Override
    @Transactional
    public void deleteUserAndTeacher(Long userId) {
        // 1) Fetch user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) Fetch linked teacher
        Teacher teacher = user.getTeacher();

        // 3) Unlink and Delete teacher first to avoid constraint issues if user is
        // deleted first
        if (teacher != null) {
            user.setTeacher(null);
            userRepo.save(user);
            teacherRepo.delete(teacher);
        }

        // 4) Delete user (will cascade to notifications due to OneToMany cascade =
        // CascadeType.ALL)
        userRepo.delete(user);
    }

    @Override
    public List<FacultyResponse> getFacultyList() {
        // Fetch all users who have a teacher profile
        List<User> users = userRepo.findAll();

        List<FacultyResponse> list = new ArrayList<>();

        for (User u : users) {
            if (u.getTeacher() == null)
                continue;

            Teacher t = u.getTeacher();
            FacultyResponse dto = new FacultyResponse();

            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setEmail(u.getEmail());
            dto.setRole(u.getRole() != null ? u.getRole().name() : "TEACHER");
            dto.setIsActive(u.getIsActive() != null ? u.getIsActive() : false);
            dto.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");

            dto.setTeacherId(t.getId());
            dto.setName(t.getName());
            dto.setDesignation(t.getDesignation());
            dto.setDepartmentId(t.getDepartment() != null ? t.getDepartment().getId() : null);
            dto.setDepartmentName(t.getDepartment() != null ? t.getDepartment().getName() : null);
            dto.setContactNumber(t.getContactNumber());

            list.add(dto);
        }

        return list;
    }

    @Override
    public List<Teacher> getHodList() {

        List<User> hodUsers = userRepo.findByRole(User.userRole.HOD);

        List<Teacher> list = new ArrayList<>();

        for (User u : hodUsers) {
            if (u.getTeacher() != null) {
                list.add(u.getTeacher());
            }
        }

        return list;
    }

}
