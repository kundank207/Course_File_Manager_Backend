package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.User;

import java.util.List;

public interface UserService {

    User registerTeacherUser(String name, String email, String password);

    User login(String email, String password);

    org.springframework.data.domain.Page<User> getPendingTeachers(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<User> getAllTeachers(org.springframework.data.domain.Pageable pageable);

    User approveUser(Long userId);

    void deleteUser(Long userId);

    User findByEmail(String email);

    User findById(Long id);

    User updateUser(User user);

    List<String> getEffectiveRoles(User user);

}
