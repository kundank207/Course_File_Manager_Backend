package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Model.User.userRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByTeacherId(Long teacherId);

    org.springframework.data.domain.Page<User> findByRoleAndIsActive(userRole role, Boolean isActive, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<User> findByIsActiveTrue(org.springframework.data.domain.Pageable pageable);

    long countByIsActiveTrue();

    long countByRole(User.userRole role);

    org.springframework.data.domain.Page<User> findByRole(User.userRole role, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<User> findByRoleIn(java.util.List<User.userRole> roles, org.springframework.data.domain.Pageable pageable);

    Optional<User> findByTeacher(Teacher teacher);

    Optional<User> findByTeacher_Id(Long teacherId);

    List<User> findByTeacher_DepartmentId(Long departmentId);

    long countByLastLoginAfter(java.time.LocalDateTime time);

    @org.springframework.data.jpa.repository.Query("SELECT FUNCTION('YEAR', u.createdAt) as year, FUNCTION('MONTH', u.createdAt) as month, COUNT(u) "
            +
            "FROM User u WHERE u.createdAt IS NOT NULL " +
            "GROUP BY FUNCTION('YEAR', u.createdAt), FUNCTION('MONTH', u.createdAt) " +
            "ORDER BY FUNCTION('YEAR', u.createdAt) DESC, FUNCTION('MONTH', u.createdAt) DESC")
    List<Object[]> getGrowthStats();
}
