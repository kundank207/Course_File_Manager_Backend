package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.Activity_Log;

@Repository
public interface ActivityLogRepository extends JpaRepository<Activity_Log, Long> {
        List<Activity_Log> findByActor_Id(Long teacherId);

        List<Activity_Log> findTop50ByOrderByCreatedAtDesc();

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(l) FROM Activity_Log l WHERE l.actor.department.id = :deptId AND l.createdAt >= :start AND l.createdAt <= :end")
        long countByDepartmentAndInterval(@org.springframework.data.repository.query.Param("deptId") Long deptId,
                        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT l.actor.id) FROM Activity_Log l WHERE l.actor.department.id = :deptId AND l.createdAt >= :since")
        long countActiveTeachersInDepartment(@org.springframework.data.repository.query.Param("deptId") Long deptId,
                        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

        @org.springframework.data.jpa.repository.Query("SELECT DATE(l.createdAt) as date, COUNT(l) as count FROM Activity_Log l "
                        +
                        "WHERE l.actor.department.id = :deptId AND l.createdAt >= :since " +
                        "GROUP BY DATE(l.createdAt) ORDER BY DATE(l.createdAt) ASC")
        List<Object[]> findDailyActivityCounts(@org.springframework.data.repository.query.Param("deptId") Long deptId,
                        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
