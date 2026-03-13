package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
import com.mitmeerut.CFM_Portal.Repository.HeadingRepository;
import com.mitmeerut.CFM_Portal.Repository.DocumentRepository;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional(readOnly = true)
public class CourseArchiveServiceImpl implements CourseArchiveService {

    private final CourseFileRepository courseFileRepo;
    private final HeadingRepository headingRepo;
    private final DocumentRepository documentRepo;
    private final ActivityLogService activityLogService;
    private final CourseFileService courseFileService;
    private final DocumentService documentService;

    public CourseArchiveServiceImpl(CourseFileRepository courseFileRepo,
            HeadingRepository headingRepo,
            DocumentRepository documentRepo,
            ActivityLogService activityLogService,
            CourseFileService courseFileService,
            DocumentService documentService) {
        this.courseFileRepo = courseFileRepo;
        this.headingRepo = headingRepo;
        this.documentRepo = documentRepo;
        this.activityLogService = activityLogService;
        this.courseFileService = courseFileService;
        this.documentService = documentService;
    }

    @Override
    public void validateAccess(Long courseFileId, CustomUserDetails user) {
        courseFileService.validateAccess(courseFileId, user);
    }

    @Override
    @Transactional
    public void downloadArchive(Long courseFileId, CustomUserDetails user, OutputStream outputStream)
            throws IOException {
        CourseFile cf = courseFileRepo.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("Course file not found"));

        // Log the download
        String courseSummary = (cf.getCourse() != null) ? (cf.getCourse().getCode() + " - " + cf.getCourse().getTitle())
                : "Course Portfolio";
        activityLogService.logAction(user.getTeacher(), "DOWNLOAD_ARCHIVE", "CourseFile", cf.getId(),
                "Archive generated for " + courseSummary);

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            // 1. Generate Metadata / HTML files
            generateIndexHtml(cf, zos);
            generateCss(zos);

            // 2. Add Documents
            List<Heading> rootHeadings = headingRepo
                    .findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(courseFileId);
            for (Heading heading : rootHeadings) {
                traverseAndAddFiles(heading, "", zos);
            }
        }
    }

    private void traverseAndAddFiles(Heading heading, String pathPrefix, ZipOutputStream zos) throws IOException {
        String sanitizedTitle = sanitize(heading.getTitle());
        String currentPath = pathPrefix + sanitizedTitle + "/";

        // Add documents in this heading
        List<Document> docs = documentRepo.findByHeading_IdAndIsActiveTrue(heading.getId());
        for (Document doc : docs) {
            String filePathString = doc.getFilePath();
            if (filePathString != null) {
                // Use relative path resolution
                Path path = documentService.resolvePath(filePathString);
                if (Files.exists(path)) {
                    // Use ID prefix in ZIP to avoid naming conflicts if two files have same
                    // original name
                    String zipEntryPath = currentPath + doc.getId() + "_" + doc.getFileName();
                    zos.putNextEntry(new ZipEntry(zipEntryPath));
                    try (InputStream fis = Files.newInputStream(path)) {
                        fis.transferTo(zos);
                    }
                    zos.closeEntry();
                }
            }
        }

        // Recursive call for sub-headings
        List<Heading> children = headingRepo.findByParentHeadingIdOrderByOrderIndexAsc(heading.getId());
        for (Heading child : children) {
            traverseAndAddFiles(child, currentPath, zos);
        }
    }

    private void generateIndexHtml(CourseFile cf, ZipOutputStream zos) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Course Portfolio</title>");
        html.append("<link rel='stylesheet' href='assets/style.css'>");
        html.append(
                "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css'>");
        html.append("</head><body>");

        // 1. Sidebar Nav
        html.append("<div class='sidebar'>");
        html.append("<div class='close-btn'><i class='fas fa-times'></i> Close View</div>");
        List<Heading> rootHeadingsSide = headingRepo
                .findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(cf.getId());
        int counter = 1;
        for (Heading h : rootHeadingsSide) {
            String linkId = "heading-" + h.getId();
            html.append("<a href='#").append(linkId).append("' class='side-link'>").append(counter++).append(". ")
                    .append(escapeHtml(h.getTitle())).append("</a>");
        }
        html.append("</div>");

        // 2. Main Content
        html.append("<div class='main-content'>");
        html.append("<div class='page-header'>");
        html.append("<h1>Course File</h1>");
        String courseTitle = (cf.getCourse() != null) ? escapeHtml(cf.getCourse().getTitle()) : "N/A";
        String courseCode = (cf.getCourse() != null) ? escapeHtml(cf.getCourse().getCode()) : "N/A";
        String teacherName = (cf.getCreatedBy() != null) ? escapeHtml(cf.getCreatedBy().getName()) : "N/A";

        html.append("<p class='course-info'>Course Name: ").append(courseCode).append(" - ").append(courseTitle)
                .append("</p>");
        html.append("<p class='course-info'>Course Instructor: ").append(teacherName).append("</p>");
        html.append("</div>");

        List<Heading> rootHeadingsMain = headingRepo
                .findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(cf.getId());
        int mainCounter = 1;
        for (Heading h : rootHeadingsMain) {
            buildHtmlTree(h, html, "", mainCounter++ + ".");
        }

        html.append("</div>"); // end main-content
        html.append("</body></html>");

        zos.putNextEntry(new ZipEntry("index.html"));
        zos.write(html.toString().getBytes());
        zos.closeEntry();
    }

    private void buildHtmlTree(Heading heading, StringBuilder html, String pathPrefix, String numberPrefix) {
        String sanitizedTitle = sanitize(heading.getTitle());
        String currentPath = pathPrefix + sanitizedTitle + "/";
        String linkId = "heading-" + heading.getId();

        html.append("<div class='heading-row' id='").append(linkId).append("'>");
        html.append("<h2 class='unit-title'>").append(numberPrefix).append(" ").append(escapeHtml(heading.getTitle()))
                .append("</h2>");

        List<Document> docs = documentRepo.findByHeading_IdAndIsActiveTrue(heading.getId());
        for (Document doc : docs) {
            String displayFileName = escapeHtml(doc.getFileName());
            String zipEntryName = doc.getId() + "_" + doc.getFileName(); // Match the traversal name

            html.append("<div class='doc-card'>");
            html.append("<div class='doc-name'><i class='fas fa-file-pdf'></i> ").append(displayFileName)
                    .append("</div>");
            html.append("<div class='doc-actions'>");
            html.append("<a href='").append(currentPath).append(zipEntryName)
                    .append("' target='_blank' class='btn btn-open'><i class='fas fa-external-link-alt'></i> Open</a>");
            html.append("<a href='").append(currentPath).append(zipEntryName)
                    .append("' download class='btn btn-download'><i class='fas fa-download'></i> Download</a>");
            html.append("</div></div>");
        }

        List<Heading> children = headingRepo.findByParentHeadingIdOrderByOrderIndexAsc(heading.getId());
        int subCounter = 1;
        for (Heading child : children) {
            buildHtmlTree(child, html, currentPath, numberPrefix.replace(".", "") + "." + subCounter++ + ".");
        }
        html.append("</div>");
    }

    private void generateCss(ZipOutputStream zos) throws IOException {
        String css = "html { scroll-behavior: smooth; }\n" +
                "body { font-family: 'Segoe UI', system-ui, sans-serif; background: #fff; color: #333; margin: 0; display: flex; }\n"
                +
                ".sidebar { width: 260px; height: 100vh; background: #2c3e50; color: #ecf0f1; position: fixed; border-right: 1px solid #ddd; overflow-y: auto; }\n"
                +
                ".close-btn { padding: 20px; font-size: 14px; border-bottom: 1px solid #34495e; cursor: pointer; text-transform: uppercase; }\n"
                +
                ".side-link { display: block; padding: 15px 20px; font-size: 16px; border-bottom: 1px solid #34495e; transition: background 0.2s; cursor: pointer; text-decoration: none; color: #ecf0f1; }\n"
                +
                ".side-link:hover { background: #34495e; color: #3498db; }\n" +
                ".main-content { margin-left: 260px; flex: 1; padding: 40px; min-height: 100vh; }\n" +
                ".page-header { border-bottom: 3px solid #3498db; margin-bottom: 30px; padding-bottom: 10px; }\n" +
                "h1 { color: #2c3e50; margin: 0 0 10px; font-weight: 800; }\n" +
                ".course-info { color: #555; margin: 5px 0; font-size: 16px; font-weight: 500; }\n" +
                ".heading-row { margin-bottom: 50px; scroll-margin-top: 40px; }\n" +
                ".unit-title { font-size: 24px; color: #2980b9; border-bottom: 1px solid #eee; padding-bottom: 10px; margin: 30px 0 15px; }\n"
                +
                ".doc-card { display: flex; align-items: center; justify-content: space-between; background: #fdfdfd; border: 1.5px solid #eee; border-left: 5px solid #3498db; border-radius: 8px; padding: 12px 20px; margin-bottom: 15px; max-width: 700px; transition: transform 0.2s; }\n"
                +
                ".doc-card:hover { transform: translateX(5px); border-color: #3498db; }\n" +
                ".doc-name { display: flex; align-items: center; gap: 10px; color: #2c3e50; font-weight: 500; font-size: 15px; }\n"
                +
                ".doc-name i { color: #e74c3c; font-size: 18px; }\n" +
                ".doc-actions { display: flex; gap: 10px; }\n" +
                ".btn { padding: 6px 15px; border-radius: 6px; font-size: 13px; text-decoration: none; display: flex; align-items: center; gap: 6px; font-weight: 600; cursor: pointer; transition: all 0.2s; }\n"
                +
                ".btn-open { background: #3498db; color: white; }\n" +
                ".btn-download { background: #27ae60; color: white; }\n" +
                ".btn:hover { filter: brightness(1.1); scale: 1.05; }";

        zos.putNextEntry(new ZipEntry("assets/style.css"));
        zos.write(css.getBytes());
        zos.closeEntry();
    }

    private String sanitize(String name) {
        if (name == null)
            return "unknown";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
