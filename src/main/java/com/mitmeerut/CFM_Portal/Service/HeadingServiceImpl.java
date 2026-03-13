package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;
import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.Heading;
import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
import com.mitmeerut.CFM_Portal.Repository.HeadingRepository;
import com.mitmeerut.CFM_Portal.Repository.DocumentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class HeadingServiceImpl implements HeadingService {

    private final HeadingRepository headingRepo;
    private final CourseFileRepository courseFileRepo;
    private final DocumentRepository documentRepo;
    private final DocumentService documentService;
    private final ChangeLogService changeLogService;
    private final com.mitmeerut.CFM_Portal.Repository.TeacherRepository teacherRepo;

    @Autowired
    public HeadingServiceImpl(HeadingRepository headingRepo, CourseFileRepository courseFileRepo,
            DocumentRepository documentRepo, DocumentService documentService,
            ChangeLogService changeLogService, com.mitmeerut.CFM_Portal.Repository.TeacherRepository teacherRepo) {
        this.headingRepo = headingRepo;
        this.courseFileRepo = courseFileRepo;
        this.documentRepo = documentRepo;
        this.documentService = documentService;
        this.changeLogService = changeLogService;
        this.teacherRepo = teacherRepo;
    }

    @Override
    public Heading createHeading(Long courseFileId, Long parentHeadingId, String title, Integer orderIndex,
            Long teacherId) {
        CourseFile courseFile = courseFileRepo.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("CourseFile not found"));

        validateEditableStatus(courseFile);

        Heading heading = new Heading();
        heading.setCourseFile(courseFile);
        heading.setTitle(title);
        heading.setOrderIndex(orderIndex != null ? orderIndex : 1);
        heading.setCreatedAt(LocalDateTime.now());

        if (parentHeadingId != null) {
            Heading parent = headingRepo.findById(parentHeadingId)
                    .orElseThrow(() -> new RuntimeException("Parent heading not found"));
            heading.setParentHeading(parent);
        }

        Heading saved = headingRepo.save(heading);

        // Log the change
        teacherRepo.findById(teacherId).ifPresent(teacher -> {
            changeLogService.logChange(courseFile, teacher, "Added", "Structure", "Added sub-section: " + title);
        });

        return saved;
    }

    @Override
    public Heading updateHeading(Long id, String title, Long teacherId) {
        Heading heading = headingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Heading not found"));

        if (heading.getCourseFile() != null) {
            validateEditableStatus(heading.getCourseFile());
        }

        String oldTitle = heading.getTitle();
        heading.setTitle(title);
        Heading saved = headingRepo.save(heading);

        // Log the change
        teacherRepo.findById(teacherId).ifPresent(teacher -> {
            changeLogService.logChange(heading.getCourseFile(), teacher, "Modified", "Structure",
                    "Renamed section from '" + oldTitle + "' to '" + title + "'");
        });

        return saved;
    }

    @Override
    public void deleteHeading(Long id, Long teacherId) {
        Heading heading = headingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Heading not found"));

        if (heading.getCourseFile() != null) {
            validateEditableStatus(heading.getCourseFile());
        }

        String title = heading.getTitle();
        CourseFile courseFile = heading.getCourseFile();

        // Delete all child headings recursively
        deleteChildrenRecursively(id, teacherId);

        // Delete documents for this heading
        deleteDocumentsForHeading(id, teacherId);

        // Delete the heading itself
        headingRepo.delete(heading);

        // Log the change
        teacherRepo.findById(teacherId).ifPresent(teacher -> {
            changeLogService.logChange(courseFile, teacher, "Removed", "Structure", "Removed section: " + title);
        });
    }

    private void deleteChildrenRecursively(Long parentId, Long teacherId) {
        List<Heading> children = headingRepo.findByParentHeadingIdOrderByOrderIndexAsc(parentId);
        for (Heading child : children) {
            deleteChildrenRecursively(child.getId(), teacherId);
            deleteDocumentsForHeading(child.getId(), teacherId);
            headingRepo.delete(child);
        }
    }

    private void deleteDocumentsForHeading(Long headingId, Long teacherId) {
        // Fetch and delete all documents attached to this heading
        List<com.mitmeerut.CFM_Portal.Model.Document> documents = documentRepo.findByHeading_Id(headingId);
        for (com.mitmeerut.CFM_Portal.Model.Document doc : documents) {
            documentService.deleteDocument(doc.getId(), teacherId);
        }
    }

    @Override
    public List<Heading> getHeadingsByCourseFile(Long courseFileId) {
        return headingRepo.findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(courseFileId);
    }

    @Override
    public List<Heading> getChildHeadings(Long parentId) {
        return headingRepo.findByParentHeadingIdOrderByOrderIndexAsc(parentId);
    }

    private void validateEditableStatus(CourseFile cf) {
        ApprovalStatus status = cf.getStatus();
        if (status == ApprovalStatus.SUBMITTED ||
                status == ApprovalStatus.UNDER_REVIEW_HOD ||
                status == ApprovalStatus.APPROVED) {
            throw new RuntimeException("Changes are not allowed because the course file is " + status
                    + ". Please create a new revision if needed.");
        }
    }

    @Override
    public List<Heading> getAllHeadingsForFile(Long courseFileId) {
        return headingRepo.findByCourseFileId(courseFileId);
    }
}