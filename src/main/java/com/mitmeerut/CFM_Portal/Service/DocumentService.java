package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Document;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface DocumentService {

    Document uploadDocument(Long headingId, MultipartFile file, Long teacherId, String courseCode);

    void deleteDocument(Long id, Long teacherId);

    List<Document> getDocumentsByHeading(Long headingId);

    Document getDocumentById(Long id);

    List<Document> getDocumentsByCourseFile(Long courseFileId);

    // Workflow Methods
    void submitForReview(Long documentId);

    void approveDocument(Long documentId, Long reviewerId, String feedback);

    void requestChanges(Long documentId, Long reviewerId, String feedback);

    void rejectDocument(Long documentId, Long reviewerId, String feedback);

    List<Document> getDocumentsByHeadingAndRole(Long headingId, String role);

    Document resubmitDocument(Long documentId, MultipartFile file, String teacherNote, Long teacherId);

    void processAutoApprovals();

    java.nio.file.Path resolvePath(String storedPath);
}
