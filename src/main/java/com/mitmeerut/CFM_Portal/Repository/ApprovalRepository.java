package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.mitmeerut.CFM_Portal.Model.Approval;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByCourseFile_Id(Long courseFileId);

    List<Approval> findByApprover_Id(Long approverId);

    List<Approval> findByStage(String stage);

    List<Approval> findByStageAndStatus(String stage, String status);

    Optional<Approval> findTopByCourseFile_IdOrderByActedAtDesc(Long courseFileId);
}
