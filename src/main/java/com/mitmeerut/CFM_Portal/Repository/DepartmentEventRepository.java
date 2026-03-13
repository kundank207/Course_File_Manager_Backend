package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.DepartmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepartmentEventRepository extends JpaRepository<DepartmentEvent, Long> {
    List<DepartmentEvent> findByDepartmentId(Long departmentId);
}
