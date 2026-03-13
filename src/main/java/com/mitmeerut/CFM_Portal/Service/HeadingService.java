package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Heading;
import java.util.List;

public interface HeadingService {

    Heading createHeading(Long courseFileId, Long parentHeadingId, String title, Integer orderIndex, Long teacherId);

    Heading updateHeading(Long id, String title, Long teacherId);

    void deleteHeading(Long id, Long teacherId);

    List<Heading> getHeadingsByCourseFile(Long courseFileId);

    List<Heading> getChildHeadings(Long parentId);

    List<Heading> getAllHeadingsForFile(Long courseFileId);
}
