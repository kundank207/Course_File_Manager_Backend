package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.ChangeLog;
import java.util.List;

public interface ChangeLogService {
    void logChange(CourseFile courseFile, Teacher updatedBy, String type, String section, String description);

    List<ChangeLog> getLogsForFile(Long courseFileId);

    List<ChangeLog> getLogsForVersion(Long courseFileId, Integer versionId);
}
