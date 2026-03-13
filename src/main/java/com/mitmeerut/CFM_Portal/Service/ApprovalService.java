package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Approval;
import java.util.List;
import java.util.Optional;

public interface ApprovalService {
    Approval save(Approval approval);
    List<Approval> findByCourseFileId(Long courseFileId);
    Optional<Approval> findById(Long id);
    void delete(Long id);
}
