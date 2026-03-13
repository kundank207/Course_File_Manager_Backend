package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Approval;
import com.mitmeerut.CFM_Portal.Repository.ApprovalRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ApprovalServiceImpl implements ApprovalService {
    private final ApprovalRepository approvalRepository;
    public ApprovalServiceImpl(ApprovalRepository approvalRepository){ this.approvalRepository = approvalRepository; }

    @Override public Approval save(Approval approval){ return approvalRepository.save(approval); }
    @Override public List<Approval> findByCourseFileId(Long courseFileId){ return approvalRepository.findByCourseFile_Id(courseFileId); }
    @Override public Optional<Approval> findById(Long id){ return approvalRepository.findById(id); }
    @Override public void delete(Long id){ approvalRepository.deleteById(id); }
}
