package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("SELECT c FROM Course c WHERE c.programId IN (SELECT p.id FROM Program p WHERE p.department.id = :departmentId)")
    List<Course> findCoursesByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.programId IN (SELECT p.id FROM Program p WHERE p.department.id = :departmentId)")
    long countCoursesByDepartmentId(@Param("departmentId") Long departmentId);

    boolean existsByTitleAndSemesterId(String title, Long semesterId);

    boolean existsByCode(String code);

    java.util.Optional<Course> findByCode(String code);

    @Query("""
                SELECT p.department.id
                FROM Program p
                WHERE p.id = :programId
            """)
    Long findDepartmentIdByProgramId(@Param("programId") Long programId);
}
