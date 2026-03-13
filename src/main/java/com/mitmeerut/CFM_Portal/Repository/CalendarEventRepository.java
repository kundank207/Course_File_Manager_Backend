package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByDepartmentId(Long departmentId);

    @Query("SELECT e FROM CalendarEvent e WHERE e.departmentId = :deptId AND e.date >= :startDate AND e.date <= :endDate")
    List<CalendarEvent> findByDepartmentAndMonth(
            @Param("deptId") Long departmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT e FROM CalendarEvent e WHERE e.departmentId = :deptId AND e.date >= :today ORDER BY e.date ASC")
    List<CalendarEvent> findUpcomingEvents(@Param("deptId") Long departmentId, @Param("today") LocalDate today);
}
