package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.dto.FacultyResponse;
import com.mitmeerut.CFM_Portal.dto.FacultyUpdateRequest;

import java.util.List;

public interface TeacherService {

    Teacher getTeacherByUserId(Long userId);

    // UPDATED method signature
    User updateUserAndTeacher(Long userId, FacultyUpdateRequest dto);

    void deleteUserAndTeacher(Long userId);

    List<FacultyResponse> getFacultyList();

    List<Teacher> getHodList();
}
