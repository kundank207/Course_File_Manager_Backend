package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    private final TeacherRepository teacherRepository;

    private final CourseFileService courseFileService;

    public CommentServiceImpl(CommentRepository commentRepository,
            DocumentRepository documentRepository,
            TeacherRepository teacherRepository,
            CourseFileService courseFileService) {
        this.commentRepository = commentRepository;
        this.documentRepository = documentRepository;
        this.teacherRepository = teacherRepository;
        this.courseFileService = courseFileService;
    }

    @Override
    public List<Comment> findByHeadingId(Long headingId) {
        return commentRepository.findByHeading_Id(headingId);
    }

    @Override
    public List<Comment> findByCourseFileId(Long courseFileId) {
        return commentRepository.findByCourseFile_Id(courseFileId);
    }

    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        commentRepository.deleteById(id);
    }

    @Override
    public void addComment(Long documentId, String text, String role, Long authorId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Teacher author = teacherRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found"));

        Comment comment = new Comment();
        comment.setDocument(document);
        comment.setText(text);
        comment.setAuthor(author);
        comment.setCourseFile(document.getHeading().getCourseFile());
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Override
    public void validateAccess(Long courseFileId, CustomUserDetails user) {
        courseFileService.validateAccess(courseFileId, user);
    }
}
