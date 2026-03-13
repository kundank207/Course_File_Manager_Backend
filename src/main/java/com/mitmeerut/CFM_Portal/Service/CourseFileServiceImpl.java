package com.mitmeerut.CFM_Portal.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;
import com.mitmeerut.CFM_Portal.Model.Approval;
import com.mitmeerut.CFM_Portal.Model.Course;
import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.CourseTeacher;
import com.mitmeerut.CFM_Portal.Model.Heading;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.Template;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Model.Comment;
import com.mitmeerut.CFM_Portal.Model.Notification;
import com.mitmeerut.CFM_Portal.Model.Document;
import com.mitmeerut.CFM_Portal.Repository.CommentRepository;
import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
import com.mitmeerut.CFM_Portal.Repository.CourseRepository;
import com.mitmeerut.CFM_Portal.Repository.CourseTeacherRepository;
import com.mitmeerut.CFM_Portal.Repository.DocumentRepository;
import com.mitmeerut.CFM_Portal.Repository.HeadingRepository;
import com.mitmeerut.CFM_Portal.Repository.NotificationRepository;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import com.mitmeerut.CFM_Portal.Repository.TemplateRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import com.mitmeerut.CFM_Portal.Repository.DepartmentRepository;
import com.mitmeerut.CFM_Portal.Model.CourseFileShare;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseFileServiceImpl implements CourseFileService {

    private final CourseFileRepository courseFileRepo;
    private final HeadingRepository headingRepo;
    private final CourseRepository courseRepo;
    private final TemplateRepository templateRepo;
    private final TeacherRepository teacherRepo;
    private final CommentRepository commentRepo;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DocumentRepository documentRepo;
    private final DepartmentRepository departmentRepo;
    private final CourseTeacherRepository courseTeacherRepo;
    private final com.mitmeerut.CFM_Portal.Repository.ApprovalRepository approvalRepo;
    private final com.mitmeerut.CFM_Portal.Repository.CourseFileShareRepository courseFileShareRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public CourseFileServiceImpl(CourseFileRepository courseFileRepo,
            HeadingRepository headingRepo,
            CourseRepository courseRepo,
            TemplateRepository templateRepo,
            CourseTeacherRepository courseTeacherRepo,
            TeacherRepository teacherRepo,
            CommentRepository commentRepo,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            DocumentRepository documentRepo,
            DepartmentRepository departmentRepo,
            com.mitmeerut.CFM_Portal.Repository.CourseFileShareRepository courseFileShareRepo,
            com.mitmeerut.CFM_Portal.Repository.ApprovalRepository approvalRepo) {
        this.courseFileRepo = courseFileRepo;
        this.headingRepo = headingRepo;
        this.courseRepo = courseRepo;
        this.templateRepo = templateRepo;
        this.teacherRepo = teacherRepo;
        this.commentRepo = commentRepo;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.courseTeacherRepo = courseTeacherRepo;
        this.documentRepo = documentRepo;
        this.departmentRepo = departmentRepo;
        this.courseFileShareRepo = courseFileShareRepo;
        this.approvalRepo = approvalRepo;
    }

    @Override
    public CourseFile createCourseFile(com.mitmeerut.CFM_Portal.dto.CreateCourseFileRequest request,
            CustomUserDetails user) {
        User.userRole role = user.getRole();
        if (role != User.userRole.TEACHER && role != User.userRole.HOD && role != User.userRole.SUBJECTHEAD) {
            throw new RuntimeException("Only Teacher, HOD, or Subject Head can create course file");
        }
        Teacher teacher = user.getTeacher();
        if (teacher == null || teacher.getDepartment() == null) {
            throw new RuntimeException("Teacher or Department missing");
        }
        Course course = courseRepo.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("course not found"));

        String academicYear = request.getAcademicYear() != null ? request.getAcademicYear().trim() : "";
        String section = request.getSection() != null ? request.getSection().trim() : "";

        Template template = templateRepo.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Optional<CourseFile> existingFile = courseFileRepo
                .findTopByCourseIdAndAcademicYearAndSectionAndCreatedByIdOrderByRevisionNumberDesc(
                        course.getId(), academicYear, section, teacher.getId());

        if (existingFile.isPresent()) {
            CourseFile ef = existingFile.get();
            if (ef.getStatus() != ApprovalStatus.REJECTED) {
                if (ef.getStatus() == ApprovalStatus.APPROVED || ef.getStatus() == ApprovalStatus.FINAL_APPROVED) {
                    throw new RuntimeException(
                            "A course file for this course is already approved. Please go to 'My Courses' to create a new revision/version.");
                } else {
                    throw new RuntimeException(
                            "The course file for this course is already created and is currently in the "
                                    + ef.getStatus() + " stage.");
                }
            }
        }

        CourseFile courseFile = new CourseFile();
        courseFile.setCourse(course);
        courseFile.setTemplate(template);
        courseFile.setCreatedBy(teacher);
        courseFile.setAcademicYear(academicYear);
        courseFile.setSection(section);
        courseFile.setStatus(ApprovalStatus.DRAFT);
        courseFile.setCreatedAt(LocalDateTime.now());

        Optional<CourseFile> latestRevFile = courseFileRepo
                .findTopByCourseIdAndAcademicYearAndSectionOrderByRevisionNumberDesc(
                        course.getId(), request.getAcademicYear(), request.getSection());
        courseFile.setRevisionNumber((latestRevFile.isPresent() ? latestRevFile.get().getRevisionNumber() : 0) + 1);

        courseFile = courseFileRepo.save(courseFile);

        createRootHeadings(courseFile, template);
        return courseFile;
    }

    public void createRootHeadings(CourseFile courseFile, Template template) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(template.getStructure());
            com.fasterxml.jackson.databind.JsonNode headings = root.get("headings");

            if (headings != null && headings.isArray()) {
                int order = 1;
                for (com.fasterxml.jackson.databind.JsonNode node : headings) {
                    String title = node.get("title").asText();

                    Heading heading = new Heading();
                    heading.setCourseFile(courseFile);
                    heading.setTitle(title);
                    heading.setParentHeading(null);
                    heading.setOrderIndex(order++);
                    heading.setCreatedAt(LocalDateTime.now());
                    headingRepo.save(heading);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing template structure: " + e.getMessage());
        }
    }

    @Override
    public List<CourseFile> getCourseFilesByTeacher(Long teacherId) {
        return courseFileRepo.findByCreatedById(teacherId);
    }

    @Override
    @Transactional
    public void deleteCourseFile(Long id, Long teacherId) {
        CourseFile courseFile = courseFileRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Course File not found"));

        if (!courseFile.getCreatedBy().getId().equals(teacherId)) {
            throw new RuntimeException("Permission Denied: Only the creator can delete this course file.");
        }

        if (courseFile.getStatus() == ApprovalStatus.FINAL_APPROVED ||
                courseFile.getStatus() == ApprovalStatus.SUBMITTED ||
                courseFile.getStatus() == ApprovalStatus.UNDER_REVIEW_HOD) {
            throw new RuntimeException(
                    "This course file is locked and cannot be deleted while under review or approved.");
        }

        List<CourseFileShare> shares = courseFileShareRepo.findByCourseFile_Id(id);
        if (shares != null && !shares.isEmpty()) {
            courseFileShareRepo.deleteAll(shares);
        }

        List<Approval> approvals = approvalRepo.findByCourseFile_Id(id);
        if (approvals != null && !approvals.isEmpty()) {
            approvalRepo.deleteAll(approvals);
        }

        List<Comment> comments = commentRepo.findByCourseFile_Id(id);
        if (comments != null && !comments.isEmpty()) {
            commentRepo.deleteAll(comments);
        }

        List<Heading> rootHeadings = headingRepo
                .findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(courseFile.getId());
        for (Heading heading : rootHeadings) {
            deleteHeadingRecursivelyWithDocs(heading);
        }

        courseFileRepo.delete(courseFile);
    }

    private void deleteHeadingRecursivelyWithDocs(Heading heading) {
        List<Heading> children = headingRepo.findByParentHeadingIdOrderByOrderIndexAsc(heading.getId());
        for (Heading child : children) {
            deleteHeadingRecursivelyWithDocs(child);
        }

        List<Document> documents = documentRepo.findByHeading_Id(heading.getId());
        if (documents != null && !documents.isEmpty()) {
            documentRepo.deleteAll(documents);
        }

        headingRepo.delete(heading);
    }

    @Override
    public CourseFile submitFile(Long fileId) {
        CourseFile file = courseFileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (file.getStatus() == ApprovalStatus.APPROVED || file.getStatus() == ApprovalStatus.FINAL_APPROVED) {
            throw new IllegalStateException("Cannot submit an already approved file");
        }

        String academicYear = file.getAcademicYear() != null ? file.getAcademicYear().trim() : "";
        Long courseId = file.getCourse().getId();

        List<CourseTeacher> subjectHeads = courseTeacherRepo.findByCourseIdAndIsSubjectHeadTrue(courseId);

        List<CourseTeacher> matchingSH = subjectHeads.stream()
                .filter(sh -> {
                    if (sh.getAcademicYear() == null)
                        return false;
                    String shYear = sh.getAcademicYear().replaceAll("[^0-9]", "");
                    String targetYear = academicYear.replaceAll("[^0-9]", "");

                    if (shYear.isEmpty() || targetYear.isEmpty())
                        return false;

                    if (shYear.equals(targetYear))
                        return true;

                    if (shYear.startsWith(targetYear.substring(0, Math.min(4, targetYear.length()))) ||
                            targetYear.startsWith(shYear.substring(0, Math.min(4, shYear.length())))) {
                        return true;
                    }

                    return false;
                })
                .collect(Collectors.toList());

        if (matchingSH.isEmpty()) {
            file.setStatus(ApprovalStatus.UNDER_REVIEW_HOD);
            file.setSystemComment("SYSTEM_DIRECT: No Subject Head assigned for this course in " + academicYear + ".");

            Teacher creator = file.getCreatedBy();
            if (creator != null && creator.getDepartment() != null && creator.getDepartment().getHod() != null) {
                userRepository.findByTeacher_Id(creator.getDepartment().getHod().getId())
                        .ifPresent(hodUser -> {
                            createNotification(hodUser, "HOD_DIRECT_APPROVAL_REQUIRED",
                                    "Course File for " + file.getCourse().getTitle()
                                            + " routed directly to you (No SH assigned).");
                        });
            }
        } else {
            file.setStatus(ApprovalStatus.SUBMITTED);
            file.setSystemComment("FORWARDED_TO_SH: Submitted for Subject Head review.");

            for (CourseTeacher sh : matchingSH) {
                userRepository.findByTeacher_Id(sh.getTeacher().getId())
                        .ifPresent(u -> {
                            createNotification(u, "COURSE_FILE_SUBMITTED",
                                    "New course file for " + file.getCourse().getTitle() + " (" + file.getSection()
                                            + ") submitted for your review.");
                        });
            }
        }

        try {
            List<Document> documents = documentRepo.findByHeading_CourseFile_Id(file.getId());
            for (Document doc : documents) {
                if (Boolean.TRUE.equals(doc.getIsActive())) {
                    doc.setStatus(com.mitmeerut.CFM_Portal.Model.DocumentStatus.PENDING_REVIEW);
                    doc.setSubmittedAt(LocalDateTime.now());
                    documentRepo.save(doc);
                }
            }
        } catch (Exception e) {
        }

        return courseFileRepo.save(file);
    }

    @Override
    public CourseFile approveFile(Long fileId, Long approverId) {
        CourseFile file = courseFileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Teacher approver = teacherRepo.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        boolean isHodApproving = userRepository.findByTeacherId(approverId)
                .map(u -> u.getRole() == User.userRole.HOD)
                .orElse(false) || departmentRepo.findByHodId(approverId).isPresent();

        if (isHodApproving && (file.getStatus() == ApprovalStatus.UNDER_REVIEW_HOD
                || file.getStatus() == ApprovalStatus.SUBMITTED)) {
            file.setStatus(ApprovalStatus.FINAL_APPROVED);
            file.setSystemComment("FINAL_APPROVED: Approved by HOD (ID: " + approverId + ")");

            userRepository.findByTeacherId(file.getCreatedBy().getId())
                    .ifPresent(u -> {
                        createNotification(u, "COURSE_FILE_APPROVED",
                                "Your course file " + file.getCourse().getTitle() + " has been FINAL APPROVED by HOD.");
                    });
        } else if (file.getStatus() == ApprovalStatus.SUBMITTED) {
            file.setStatus(ApprovalStatus.UNDER_REVIEW_HOD);
            file.setSystemComment("FORWARDED_BY_SH: " + approver.getName());

            Approval approvalLog = new Approval();
            approvalLog.setCourseFile(file);
            approvalLog.setApprover(approver);
            approvalLog.setStage("SUBJECT_HEAD");
            approvalLog.setStatus("APPROVED");
            approvalLog.setComment("Reviewed and forwarded to HOD.");
            approvalLog.setActedAt(LocalDateTime.now());
            approvalRepo.save(approvalLog);

            Teacher creator = file.getCreatedBy();
            if (creator != null && creator.getDepartment() != null && creator.getDepartment().getHod() != null) {
                userRepository.findByTeacher_Id(creator.getDepartment().getHod().getId())
                        .ifPresent(hodUser -> {
                            createNotification(hodUser, "HOD_APPROVAL_REQUIRED",
                                    "Course File for " + file.getCourse().getTitle()
                                            + " is ready for your final approval after Subject Head review.");
                        });
            }

            try {
                List<Document> docs = documentRepo.findByHeading_CourseFile_Id(file.getId());
                for (Document doc : docs) {
                    if (Boolean.TRUE.equals(doc.getIsActive()) &&
                            doc.getStatus() == com.mitmeerut.CFM_Portal.Model.DocumentStatus.PENDING_REVIEW) {
                        doc.setStatus(com.mitmeerut.CFM_Portal.Model.DocumentStatus.APPROVED);
                        doc.setReviewedAt(LocalDateTime.now());
                        doc.setReviewedBy(approver);
                        documentRepo.save(doc);
                    }
                }
            } catch (Exception e) {
            }
        } else {
            throw new IllegalStateException("You are not authorized to approve this file in its current state");
        }

        return courseFileRepo.save(file);
    }

    @Override
    public CourseFile rejectFile(Long fileId, String reason, String role, Long reviewerId) {
        CourseFile file = courseFileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        file.setStatus(ApprovalStatus.REJECTED);
        file.setRejectedByRole(role);
        file.setRejectedAt(LocalDateTime.now());
        file.setSystemComment(reason);

        CourseFile savedFile = courseFileRepo.save(file);

        Teacher reviewer = teacherRepo.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        Comment comment = new Comment();
        comment.setCourseFile(savedFile);
        comment.setAuthor(reviewer);
        comment.setText(reason);
        comment.setCreatedAt(LocalDateTime.now());
        commentRepo.save(comment);

        userRepository.findByTeacherId(savedFile.getCreatedBy().getId())
                .ifPresent(u -> {
                    createNotification(u, "COURSE_FILE_REJECTED",
                            "Your course file " + savedFile.getCourse().getTitle() + " was returned. Reason: "
                                    + reason);
                });

        return savedFile;
    }

    private void createNotification(User user, String type, String message) {
        if (user == null)
            return;
        Notification note = new Notification();
        note.setUser(user);
        note.setType(type);
        try {
            note.setPayload("{\"message\":\"" + message + "\"}");
        } catch (Exception e) {
        }
        note.setIsRead(false);
        note.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(note);
    }

    @Override
    public List<CourseFile> getCourseFilesByStatus(Long teacherId, ApprovalStatus status) {
        return courseFileRepo.findByCreatedByIdAndStatus(teacherId, status);
    }

    @Override
    public List<CourseFile> getRejectedCourseFilesByTeacher(Long teacherId) {
        return courseFileRepo.findByCreatedByIdAndStatusIn(teacherId, List.of(ApprovalStatus.REJECTED));
    }

    @Override
    public List<CourseTeacher> getSubjectHeadAssignments(Long teacherId) {
        return courseTeacherRepo.findSubjectHeadAssignmentsByTeacherId(teacherId);
    }

    @Override
    public List<CourseTeacher> getAllAssignmentsUnderSubjectHead(Long teacherId) {
        return courseTeacherRepo.findAllAssignmentsUnderSubjectHead(teacherId);
    }

    @Override
    public CourseFile createRevision(Long fileId, Long teacherId) {
        CourseFile original = courseFileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Original file not found"));

        if (!original.getCreatedBy().getId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized to create revision of this file");
        }

        CourseFile newFile = new CourseFile();
        newFile.setCourse(original.getCourse());
        newFile.setAcademicYear(original.getAcademicYear() != null ? original.getAcademicYear().trim() : "");
        newFile.setSection(original.getSection() != null ? original.getSection().trim() : "");
        newFile.setCreatedBy(original.getCreatedBy());
        newFile.setTemplate(original.getTemplate());
        newFile.setStatus(ApprovalStatus.DRAFT);

        Optional<CourseFile> latestRevFile = courseFileRepo
                .findTopByCourseIdAndAcademicYearAndSectionOrderByRevisionNumberDesc(
                        original.getCourse().getId(), original.getAcademicYear(), original.getSection());

        int nextRev = (latestRevFile.isPresent() ? latestRevFile.get().getRevisionNumber() : 0) + 1;
        newFile.setRevisionNumber(nextRev);
        newFile.setParentFileId(original.getId());
        newFile.setCreatedAt(LocalDateTime.now());

        CourseFile savedFile = courseFileRepo.save(newFile);

        List<Heading> rootHeadings = headingRepo
                .findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(original.getId());

        for (Heading root : rootHeadings) {
            copyHeadingRecursive(root, null, savedFile);
        }

        return savedFile;
    }

    private void copyHeadingRecursive(Heading oldHeading, Heading newParent, CourseFile newFile) {
        Heading newHeading = new Heading();
        newHeading.setTitle(oldHeading.getTitle());
        newHeading.setOrderIndex(oldHeading.getOrderIndex());
        newHeading.setCourseFile(newFile);
        newHeading.setParentHeading(newParent);
        newHeading.setCreatedAt(LocalDateTime.now());
        Heading savedHeading = headingRepo.save(newHeading);

        List<Document> docs = documentRepo.findByHeading_Id(oldHeading.getId());
        for (Document oldDoc : docs) {
            Document newDoc = new Document();
            newDoc.setFileName(oldDoc.getFileName());
            newDoc.setFilePath(oldDoc.getFilePath());
            newDoc.setFileSize(oldDoc.getFileSize());
            newDoc.setType(oldDoc.getType());
            newDoc.setUploadedBy(oldDoc.getUploadedBy());
            newDoc.setUploadedAt(LocalDateTime.now());
            newDoc.setHeading(savedHeading);
            documentRepo.save(newDoc);
        }

        List<Heading> children = headingRepo.findByParentHeadingIdOrderByOrderIndexAsc(oldHeading.getId());
        for (Heading child : children) {
            copyHeadingRecursive(child, savedHeading, newFile);
        }
    }

    @Override
    public List<CourseFile> findPreviousCourseFiles(Long courseId, String academicYear) {
        return courseFileRepo.findByCourse_IdAndAcademicYearNot(courseId, academicYear);
    }

    @Override
    public CourseFile reuseCourseFile(Long previousFileId, Long newCourseId, String academicYear, String section,
            Long teacherId) {
        CourseFile previous = courseFileRepo.findById(previousFileId)
                .orElseThrow(() -> new RuntimeException("Previous course file not found"));

        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        Course newCourse = courseRepo.findById(newCourseId)
                .orElseThrow(() -> new RuntimeException("New course not found"));

        CourseFile newFile = new CourseFile();
        newFile.setCourse(newCourse);
        newFile.setAcademicYear(academicYear);
        newFile.setSection(section);
        newFile.setCreatedBy(teacher);
        newFile.setStatus(ApprovalStatus.DRAFT);
        newFile.setCreatedAt(LocalDateTime.now());
        newFile.setTemplate(previous.getTemplate());

        Optional<CourseFile> existing = courseFileRepo
                .findTopByCourseIdAndAcademicYearAndSectionAndCreatedByIdOrderByRevisionNumberDesc(
                        newCourseId, academicYear, section, teacherId);

        if (existing.isPresent() && existing.get().getStatus() != ApprovalStatus.REJECTED) {
            throw new RuntimeException("A course file for this course/section already exists");
        }

        Optional<CourseFile> latestRevFile = courseFileRepo
                .findTopByCourseIdAndAcademicYearAndSectionOrderByRevisionNumberDesc(
                        newCourseId, academicYear, section);
        newFile.setRevisionNumber((latestRevFile.isPresent() ? latestRevFile.get().getRevisionNumber() : 0) + 1);

        CourseFile savedFile = courseFileRepo.save(newFile);

        List<Heading> rootHeadings = headingRepo
                .findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(previous.getId());

        for (Heading root : rootHeadings) {
            copyHeadingRecursive(root, null, savedFile);
        }

        return savedFile;
    }

    @Override
    public void shareCourseFile(Long fileId, Long sharedById, Long sharedWithId, String message) {
        CourseFile file = courseFileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Course file not found"));
        Teacher sharedBy = teacherRepo.findById(sharedById)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        Teacher sharedWith = teacherRepo.findById(sharedWithId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        CourseFileShare share = new CourseFileShare();
        share.setCourseFile(file);
        share.setSharedBy(sharedBy);
        share.setSharedWith(sharedWith);
        share.setMessage(message);
        share.setSharedAt(LocalDateTime.now());
        courseFileShareRepo.save(share);

        userRepository.findByTeacherId(sharedWithId).ifPresent(u -> {
            createNotification(u, "COURSE_FILE_SHARED",
                    sharedBy.getName() + " shared a course file with you");
        });
    }

    @Override
    public List<CourseFileShare> getSharedWithMe(Long teacherId) {
        return courseFileShareRepo.findBySharedWithId(teacherId);
    }

    @Override
    public CourseFile copySharedFile(Long shareId, String academicYear, String section, Long teacherId) {
        CourseFileShare share = courseFileShareRepo.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share record not found"));

        CourseFile original = share.getCourseFile();
        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        CourseFile newFile = new CourseFile();
        newFile.setCourse(original.getCourse());
        newFile.setAcademicYear(academicYear);
        newFile.setSection(section);
        newFile.setCreatedBy(teacher);
        newFile.setStatus(ApprovalStatus.DRAFT);
        newFile.setCreatedAt(LocalDateTime.now());
        newFile.setTemplate(original.getTemplate());

        Optional<CourseFile> latestRevFile = courseFileRepo
                .findTopByCourseIdAndAcademicYearAndSectionOrderByRevisionNumberDesc(
                        original.getCourse().getId(), academicYear, section);
        newFile.setRevisionNumber((latestRevFile.isPresent() ? latestRevFile.get().getRevisionNumber() : 0) + 1);

        CourseFile savedFile = courseFileRepo.save(newFile);

        List<Heading> rootHeadings = headingRepo
                .findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(original.getId());

        for (Heading root : rootHeadings) {
            copyHeadingRecursive(root, null, savedFile);
        }

        return savedFile;
    }

    @Override
    public long getVersionCount(Long courseId, String academicYear, String section) {
        return courseFileRepo.countByCourseIdAndAcademicYearAndSection(courseId, academicYear, section);
    }

    @Override
    public void validateAccess(Long courseFileId, CustomUserDetails user) {
        CourseFile cf = courseFileRepo.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("Course file not found"));
        validateAccess(cf, user);
    }

    @Override
    public void validateAccess(CourseFile cf, CustomUserDetails user) {
        if (user.getTeacher() == null) {
            throw new RuntimeException("403 Forbidden: No teacher profile associated with this user.");
        }

        Long teacherId = user.getTeacher().getId();
        User.userRole role = user.getRole();

        // Simple regex to check for Subject Head authority
        boolean hasSHAuthority = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUBJECTHEAD")
                        || a.getAuthority().equals("ROLE_SUBJECT_HEAD"));

        // 1. Creator ALWAYS has access
        if (cf.getCreatedBy() != null && cf.getCreatedBy().getId().equals(teacherId)) {
            return;
        }

        // 2. Assigned Instructor ALWAYS has access
        if (cf.getCourse() != null) {
            boolean isAssigned = courseTeacherRepo.existsByCourseIdAndTeacherIdAndAcademicYearAndSection(
                    cf.getCourse().getId(), teacherId, cf.getAcademicYear(), cf.getSection());
            if (isAssigned)
                return;
        }

        // 3. Shared File Check
        boolean isShared = courseFileShareRepo.findByCourseFile_Id(cf.getId()).stream()
                .anyMatch(s -> s.getSharedWith().getId().equals(teacherId));
        if (isShared) {
            return;
        }

        // 4. Subject Head Check (Assignment based)
        if (cf.getCourse() != null) {
            boolean isSHAssignment = courseTeacherRepo.findByCourseIdAndIsSubjectHeadTrue(cf.getCourse().getId())
                    .stream()
                    .anyMatch(ct -> {
                        if (ct.getTeacher() == null || !ct.getTeacher().getId().equals(teacherId))
                            return false;

                        String ctYear = (ct.getAcademicYear() != null) ? ct.getAcademicYear().trim() : "";
                        String cfYear = (cf.getAcademicYear() != null) ? cf.getAcademicYear().trim() : "";

                        if (ctYear.isEmpty() || cfYear.isEmpty())
                            return false;

                        return ctYear.equals(cfYear) || (ctYear.length() >= 4 && cfYear.length() >= 4 &&
                                ctYear.substring(0, 4).equals(cfYear.substring(0, 4)));
                    });

            if (isSHAssignment) {
                if (cf.getStatus() == ApprovalStatus.DRAFT) {
                    throw new RuntimeException(
                            "403 Forbidden: Subject Heads can only view submitted or finalized course files.");
                }
                return;
            }
        }

        // 4. HOD Check
        if (role == User.userRole.HOD) {
            try {
                if (cf.getCourse() != null && cf.getCourse().getProgram() != null &&
                        cf.getCourse().getProgram().getDepartment() != null &&
                        cf.getCourse().getProgram().getDepartment().getId().equals(user.getDepartmentId())) {

                    // HOD can view/download anything in their department that's NOT a draft
                    if (cf.getStatus() != ApprovalStatus.DRAFT) {
                        return;
                    } else {
                        throw new RuntimeException("403 Forbidden: HODs cannot view draft course files.");
                    }
                }
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("403"))
                    throw (RuntimeException) e;
            }
            throw new RuntimeException("403 Forbidden: You can only access course files from your own department.");
        } else if (role == User.userRole.SUBJECTHEAD || hasSHAuthority) {
            throw new RuntimeException(
                    "403 Forbidden: You are not assigned as Subject Head for this course in " + cf.getAcademicYear());
        } else {
            throw new RuntimeException("403 Forbidden: You do not have permission to access this course file.");
        }
    }
}
