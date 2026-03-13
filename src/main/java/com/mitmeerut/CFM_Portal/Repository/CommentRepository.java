package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByHeading_Id(Long headingId);

    List<Comment> findByCourseFile_Id(Long courseFileId);

    List<Comment> findByAuthor_Id(Long authorId);

    List<Comment> findByDocument_Id(Long documentId);

    boolean existsByDocument_Id(Long documentId);

    List<Comment> findByCourseFile_IdIn(List<Long> courseFileIds);
}
