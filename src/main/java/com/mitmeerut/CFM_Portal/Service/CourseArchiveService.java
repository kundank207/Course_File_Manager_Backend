package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;

import java.io.IOException;
import java.io.OutputStream;

public interface CourseArchiveService {
    void downloadArchive(Long courseFileId, CustomUserDetails user, OutputStream outputStream) throws IOException;

    void validateAccess(Long courseFileId, CustomUserDetails user);
}
