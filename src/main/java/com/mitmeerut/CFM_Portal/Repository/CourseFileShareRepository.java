package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.CourseFileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseFileShareRepository extends JpaRepository<CourseFileShare, Long> {
    List<CourseFileShare> findBySharedWithId(Long teacherId);

    List<CourseFileShare> findBySharedById(Long teacherId);

    List<CourseFileShare> findByCourseFile_Id(Long courseFileId);
}
