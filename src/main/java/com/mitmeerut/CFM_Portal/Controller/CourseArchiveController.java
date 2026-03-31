package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Service.CourseArchiveService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/archive")

@PreAuthorize("hasAnyRole('HOD', 'SUBJECTHEAD', 'TEACHER')")
public class CourseArchiveController {

    private final CourseArchiveService archiveService;

    public CourseArchiveController(CourseArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping("/download/{courseFileId}")
    public void downloadArchive(@PathVariable("courseFileId") Long courseFileId,
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletResponse response) throws IOException {

        try {
            // 1. Validate Access FIRST (Wait to call getOutputStream until authorized)
            archiveService.validateAccess(courseFileId, user);

            // 2. Authorization successful. Set headers.
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"Course_Archive_" + courseFileId + ".zip\"");

            // 3. Now request the stream and start writing
            archiveService.downloadArchive(courseFileId, user, response.getOutputStream());

        } catch (RuntimeException e) {
            // Log for server diagnostics
            System.err.println("Archive Access/Generation Error: " + e.getMessage());

            if (!response.isCommitted()) {
                response.setStatus(e.getMessage().contains("403 Forbidden") ? HttpServletResponse.SC_FORBIDDEN
                        : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");

                String jsonError = "{\"message\":\"" + e.getMessage().replace("\"", "\\\"").replace("\n", " ") + "\"}";
                try {
                    // Try getWriter first
                    response.getWriter().write(jsonError);
                } catch (IllegalStateException | IOException ex) {
                    // If getOutputStream was already called, use it to write the JSON error
                    try {
                        response.getOutputStream().write(jsonError.getBytes());
                    } catch (Exception ex2) {
                        // If everything fails, just let it be
                    }
                }
            }
        }
    }
}
