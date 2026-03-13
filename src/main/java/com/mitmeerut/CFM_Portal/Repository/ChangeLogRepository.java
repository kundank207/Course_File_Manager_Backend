package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.ChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {
    List<ChangeLog> findByCourseFileIdOrderByCreatedAtDesc(Long courseFileId);

    List<ChangeLog> findByCourseFileIdAndVersionIdOrderByCreatedAtDesc(Long courseFileId, Integer versionId);
}
