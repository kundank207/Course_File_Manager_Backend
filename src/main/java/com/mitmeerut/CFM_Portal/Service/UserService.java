package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.User;

import java.util.List;

public interface UserService {

    User registerTeacherUser(String name, String email, String password);

    User login(String email, String password);

    List<User> getPendingTeachers();

    List<User> getAllTeachers();

    User approveUser(Long userId);

    void deleteUser(Long userId);

    User findByEmail(String email);

    User findById(Long id);

    User updateUser(User user);

    List<String> getEffectiveRoles(User user);

}
