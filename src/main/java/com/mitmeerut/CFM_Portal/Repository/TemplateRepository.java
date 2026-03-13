package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.Template;
import com.mitmeerut.CFM_Portal.Model.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByDepartmentId(Long departmentId);

    List<Template> findByDepartmentIdAndTemplateTypeIn(Long departmentId, List<TemplateType> types);

    Optional<Template> findFirstByDepartmentId(Long departmentId);

    List<Template> findByCreatedBy(Long createdBy);

    @Query("SELECT COUNT(t) FROM Template t WHERE t.departmentId = :deptId AND t.createdAt >= :start AND t.createdAt <= :end")
    long countByDepartmentAndInterval(@Param("deptId") Long deptId, @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);
}
