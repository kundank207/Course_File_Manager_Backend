package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.Heading;

@Repository
public interface HeadingRepository extends JpaRepository<Heading, Long> {

    List<Heading> findByCourseFileIdAndParentHeadingIsNull(Long courseFileId);

    List<Heading> findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(Long courseFileId);

    List<Heading> findByParentHeadingId(Long parentId);

    List<Heading> findByParentHeadingIdOrderByOrderIndexAsc(Long parentId);

    List<Heading> findByCourseFileId(Long courseFileId);

    long countByCourseFileId(Long courseFileId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(h) FROM Heading h WHERE h.courseFile.id = :cfId AND EXISTS (SELECT 1 FROM Document d WHERE d.heading.id = h.id AND (d.isActive = true OR d.isActive IS NULL))")
    long countCompletedHeadingsByCourseFileId(@org.springframework.data.repository.query.Param("cfId") Long cfId);

    @org.springframework.data.jpa.repository.Query("SELECT h.courseFile.id, COUNT(h), SUM(CASE WHEN EXISTS (SELECT 1 FROM Document d WHERE d.heading.id = h.id AND (d.isActive = true OR d.isActive IS NULL)) THEN 1 ELSE 0 END) "
            +
            "FROM Heading h WHERE h.courseFile.id IN :cfIds GROUP BY h.courseFile.id")
    List<Object[]> findProgressStatsByCourseFileIds(
            @org.springframework.data.repository.query.Param("cfIds") List<Long> cfIds);
}
