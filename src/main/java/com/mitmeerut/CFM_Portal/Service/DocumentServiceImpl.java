package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepo;
    private final HeadingRepository headingRepo;
    private final TeacherRepository teacherRepo;
    private final ChangeLogService changeLogService;

    @Value("${file.upload.base-path:storage}")
    private String baseUploadPath;

    @PostConstruct
    public void init() {
        // Dynamic path correction: if 'storage' isn't found in the current dir,
        // check if we are running from the root and it's in 'CFM_Backend/storage'
        java.nio.file.Path path = java.nio.file.Paths.get(baseUploadPath);
        if (!path.isAbsolute() && !java.nio.file.Files.exists(path)) {
            java.nio.file.Path altPath = java.nio.file.Paths.get("CFM_Backend").resolve(baseUploadPath);
            if (java.nio.file.Files.exists(altPath)) {
                baseUploadPath = altPath.toAbsolutePath().toString();
                System.out.println("System: Storage path auto-corrected to: " + baseUploadPath);
            }
        } else if (path.isAbsolute() || java.nio.file.Files.exists(path)) {
            baseUploadPath = path.toAbsolutePath().toString();
        }
    }

    @Autowired
    public DocumentServiceImpl(DocumentRepository documentRepo, HeadingRepository headingRepo,
            TeacherRepository teacherRepo, ChangeLogService changeLogService) {
        this.documentRepo = documentRepo;
        this.headingRepo = headingRepo;
        this.teacherRepo = teacherRepo;
        this.changeLogService = changeLogService;
    }

    @PostConstruct
    public void migrateExistingDocuments() {
        List<Document> documents = documentRepo.findAll();
        for (Document doc : documents) {
            boolean docChanged = false;
            if (doc.getStatus() == null) {
                doc.setStatus(DocumentStatus.DRAFT);
                docChanged = true;
            }
            if (doc.getIsActive() == null) {
                doc.setIsActive(true);
                docChanged = true;
            }
            if (doc.getVersionNo() == null) {
                doc.setVersionNo(1);
                docChanged = true;
            }
            if (docChanged) {
                documentRepo.save(doc);
            }
        }
    }

    @Override
    public Document uploadDocument(Long headingId, MultipartFile file, Long teacherId, String courseCode) {
        Heading heading = headingRepo.findById(headingId)
                .orElseThrow(() -> new RuntimeException("Heading not found"));

        if (heading.getCourseFile() != null) {
            validateDocumentEditPermission(heading.getCourseFile(), null);
        }

        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        // Build storage path - Save relative to baseUploadPath
        String sanitizedHeadingTitle = sanitizeFileName(heading.getTitle());
        String relativeDir = String.format("teachers/%d/%s/%s/",
                teacherId, courseCode, sanitizedHeadingTitle);
        Path fullDirPath = Paths.get(baseUploadPath).resolve(relativeDir);

        try {
            Files.createDirectories(fullDirPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory");
        }

        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = System.currentTimeMillis() + "_" + sanitizeFileName(originalFileName);
        Path fullPath = fullDirPath.resolve(uniqueFileName);

        try {
            Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }

        // SMART MARKING: If the course file is not in DRAFT mode,
        // this is a "Fix" upload. Mark it so the Reviewer can see it immediately.
        boolean isFix = false;
        if (heading.getCourseFile() != null) {
            ApprovalStatus s = heading.getCourseFile().getStatus();
            if (s != ApprovalStatus.DRAFT) {
                isFix = true;
            }
        }

        Document document = new Document();
        document.setHeading(heading);
        document.setUploadedBy(teacher);
        document.setFileName(originalFileName);
        // Save ONLY the relative part in database
        document.setFilePath(relativeDir.replace("\\", "/") + uniqueFileName);
        document.setType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setUploadedAt(LocalDateTime.now());
        document.setStatus(DocumentStatus.DRAFT);
        document.setVersionNo(1);
        document.setIsActive(true);
        document.setIsResubmitted(isFix); // Mark as resubmitted/fix if not a draft session

        Document saved = documentRepo.save(document);

        // Log the change
        changeLogService.logChange(heading.getCourseFile(), teacher, "Added", heading.getTitle(),
                "Uploaded document: " + originalFileName);

        return saved;
    }

    @Override
    public void submitForReview(Long documentId) {
        Document doc = getDocumentById(documentId);
        if (doc.getStatus() != DocumentStatus.DRAFT && doc.getStatus() != DocumentStatus.CHANGES_REQUESTED) {
            throw new RuntimeException("Document is already in " + doc.getStatus() + " state.");
        }

        doc.setStatus(DocumentStatus.PENDING_REVIEW);
        doc.setSubmittedAt(LocalDateTime.now());
        doc.setReviewDeadline(LocalDateTime.now().plusHours(48)); // Auto-approve after 48h
        documentRepo.save(doc);
    }

    @Override
    public void approveDocument(Long documentId, Long reviewerId, String feedback) {
        Document doc = getDocumentById(documentId);
        Teacher reviewer = teacherRepo.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        doc.setStatus(DocumentStatus.APPROVED);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(reviewer);
        doc.setReviewerFeedback(feedback);
        documentRepo.save(doc);

        // Log the approval
        changeLogService.logChange(doc.getHeading().getCourseFile(), reviewer, "Approved",
                doc.getFileName(), "Document approved with note: " + feedback);
    }

    @Override
    public void requestChanges(Long documentId, Long reviewerId, String feedback) {
        Document doc = getDocumentById(documentId);
        Teacher reviewer = teacherRepo.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        doc.setStatus(DocumentStatus.CHANGES_REQUESTED);
        doc.setIsActive(true); // Keep active so teacher can see and fix/remove it
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(reviewer);
        doc.setReviewerFeedback(feedback);
        documentRepo.save(doc);

        // Log the change
        changeLogService.logChange(doc.getHeading().getCourseFile(), reviewer, "Requested Changes",
                doc.getFileName(), "Document flagged for changes by " + reviewer.getName() + ": " + feedback);
    }

    @Override
    public void rejectDocument(Long documentId, Long reviewerId, String feedback) {
        Document doc = getDocumentById(documentId);
        Teacher reviewer = teacherRepo.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setIsActive(true); // Keep active as requested: Teacher will manually remove after seeing flag
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(reviewer);
        doc.setReviewerFeedback(feedback);
        documentRepo.save(doc);

        // Log the change
        changeLogService.logChange(doc.getHeading().getCourseFile(), reviewer, "Flagged Conflict",
                doc.getFileName(), "Document flagged for conflict by " + reviewer.getName() + ": " + feedback);
    }

    @Override
    public List<Document> getDocumentsByHeadingAndRole(Long headingId, String role) {
        switch (role.toUpperCase()) {
            case "TEACHER":
                // Teacher sees all their active documents (including flagged ones)
                return documentRepo.findByHeading_IdAndIsActiveTrue(headingId);
            case "SUBJECTHEAD":
            case "HOD":
                // Reviewers should see PENDING, APPROVED, REJECTED, and CHANGES_REQUESTED items
                // that are active
                return documentRepo.findByHeading_IdAndStatusInAndIsActiveTrue(headingId,
                        List.of(DocumentStatus.PENDING_REVIEW, DocumentStatus.APPROVED,
                                DocumentStatus.REJECTED, DocumentStatus.CHANGES_REQUESTED));
            default:
                return documentRepo.findByHeading_IdAndIsActiveTrue(headingId);
        }
    }

    @Override
    public Document resubmitDocument(Long documentId, MultipartFile file, String teacherNote, Long teacherId) {
        Document oldDoc = getDocumentById(documentId);

        // Validation bypass: Allow resubmitting if specifically flagged,
        // even if course file is otherwise locked.
        if (oldDoc.getHeading() != null && oldDoc.getHeading().getCourseFile() != null) {
            validateDocumentEditPermission(oldDoc.getHeading().getCourseFile(), oldDoc);
        }

        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (!oldDoc.getUploadedBy().getId().equals(teacherId)) {
            throw new RuntimeException("Permission Denied: Only the original uploader can resubmit.");
        }

        // Deactivate old doc
        oldDoc.setIsActive(false);
        documentRepo.save(oldDoc);

        // Upload new doc
        Heading heading = oldDoc.getHeading();
        String courseCode = heading.getCourseFile() != null ? heading.getCourseFile().getCourse().getCode() : "GEN";

        // Upload new doc - Relative path logic
        String sanitizedHeadingTitle = sanitizeFileName(heading.getTitle());
        String relativeDir = String.format("teachers/%d/%s/%s/",
                teacherId, courseCode, sanitizedHeadingTitle);
        Path fullDirPath = Paths.get(baseUploadPath).resolve(relativeDir);

        try {
            Files.createDirectories(fullDirPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory");
        }

        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = System.currentTimeMillis() + "_FIX_" + sanitizeFileName(originalFileName);
        Path fullPath = fullDirPath.resolve(uniqueFileName);

        try {
            Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save re-submitted file");
        }

        Document newDoc = new Document();
        newDoc.setHeading(heading);
        newDoc.setUploadedBy(teacher);
        newDoc.setFileName(originalFileName);
        newDoc.setFilePath(relativeDir.replace("\\", "/") + uniqueFileName);
        newDoc.setType(file.getContentType());
        newDoc.setFileSize(file.getSize());
        newDoc.setUploadedAt(LocalDateTime.now());
        newDoc.setVersionNo(oldDoc.getVersionNo() + 1);
        newDoc.setIsActive(true);
        newDoc.setIsResubmitted(true);
        newDoc.setTeacherNote(teacherNote);
        newDoc.setStatus(DocumentStatus.PENDING_REVIEW);
        newDoc.setSubmittedAt(LocalDateTime.now());

        Document saved = documentRepo.save(newDoc);

        // Log the fix
        changeLogService.logChange(heading.getCourseFile(), teacher, "Fixed Flag",
                heading.getTitle(), "Re-submitted " + originalFileName + " with note: " + teacherNote);

        return saved;
    }

    @Override
    public void processAutoApprovals() {
        LocalDateTime now = LocalDateTime.now();
        List<Document> pendingAutoApproval = documentRepo.findByStatusAndReviewDeadlineBefore(
                DocumentStatus.PENDING_REVIEW, now);

        for (Document doc : pendingAutoApproval) {
            doc.setStatus(DocumentStatus.APPROVED);
            doc.setReviewedAt(now);
            // reviewedBy remains null as it was system-approved
            documentRepo.save(doc);
        }
    }

    @Override
    public void deleteDocument(Long id, Long teacherId) {
        Document document = getDocumentById(id);

        if (document.getHeading() != null && document.getHeading().getCourseFile() != null) {
            validateDocumentEditPermission(document.getHeading().getCourseFile(), document);
        }

        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        // Permission check: Only the Original Uploader can delete the document
        if (document.getUploadedBy() != null && !document.getUploadedBy().getId().equals(teacherId)) {
            throw new RuntimeException("Permission Denied: Only the teacher who uploaded this file ("
                    + document.getUploadedBy().getName() + ") can remove it.");
        }

        // Professional requirement: Use soft delete via isActive flag
        document.setIsActive(false);
        documentRepo.save(document);

        // Log the change
        changeLogService.logChange(document.getHeading().getCourseFile(), teacher, "Removed",
                document.getHeading().getTitle(), "Removed document: " + document.getFileName());
    }

    private void validateDocumentEditPermission(CourseFile cf, Document doc) {
        // STRICT FIXING MODE:
        // If the course file is SUBMITTED, UNDER_REVIEW, or RETURNED_xxx,
        // we only allow modifications if the item is specifically FLAGGED.

        // 1. If it's an existing document being edited/deleted
        if (doc != null) {
            // ALLOW: Document is flagged
            if (doc.getStatus() == DocumentStatus.REJECTED || doc.getStatus() == DocumentStatus.CHANGES_REQUESTED) {
                return;
            }
            // ALLOW: Reviewer left a comment
            if (doc.getReviewerFeedback() != null && !doc.getReviewerFeedback().trim().isEmpty()) {
                return;
            }
            // ALLOW: This is a NEW fix uploaded during this repair cycle, allow correcting
            // mistakes
            if (Boolean.TRUE.equals(doc.getIsResubmitted()) &&
                    (doc.getStatus() == DocumentStatus.DRAFT || doc.getStatus() == DocumentStatus.PENDING_REVIEW)) {
                return;
            }
        }

        ApprovalStatus status = cf.getStatus();

        // 2. If it's a NEW upload (doc is null)
        if (doc == null && status != ApprovalStatus.DRAFT) {
            // Allow upload if status is any "RETURNED" or "REJECTED" status,
            // the UI controls visibility based on flagging.
            if (status == ApprovalStatus.REJECTED || status.name().startsWith("RETURNED"))
                return;
        }

        if (status == ApprovalStatus.FINAL_APPROVED) {
            throw new RuntimeException("This course file is already approved and cannot be edited.");
        }

        if (status != ApprovalStatus.DRAFT) {
            throw new RuntimeException("Changes are not allowed because the course file is currently " + status
                    + ". You can only fix flagged documents (Red status).");
        }
    }

    @Override
    public List<Document> getDocumentsByHeading(Long headingId) {
        return documentRepo.findByHeading_IdAndIsActiveTrue(headingId);
    }

    @Override
    public Document getDocumentById(Long id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    @Override
    public List<Document> getDocumentsByCourseFile(Long courseFileId) {
        return documentRepo.findByHeading_CourseFile_Id(courseFileId);
    }

    @Override
    public Path resolvePath(String storedPath) {
        if (storedPath == null)
            return null;

        String cleanPath = storedPath;
        // Fix for double 'storage' folder: if path already starts with storage/, remove
        // it
        if (cleanPath.startsWith("storage/") || cleanPath.startsWith("storage\\")) {
            cleanPath = cleanPath.substring(8);
        }

        // Compatibility: Check if path is absolute or relative
        Path path = Paths.get(cleanPath);
        if (path.isAbsolute()) {
            return path;
        }

        // Resolve relative path against baseUploadPath
        return Paths.get(baseUploadPath).resolve(cleanPath);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null)
            return "unnamed";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

}
