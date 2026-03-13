package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class BulkHodImportService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private com.mitmeerut.CFM_Portal.Repository.CourseTeacherRepository courseTeacherRepo;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    // ================= COURSE TEMPLATE =================

    public byte[] generateCourseTemplate() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Course Import Template");

        Row header = sheet.createRow(0);
        String[] columns = { "ProgramName", "BranchName", "SemesterNumber", "CourseCode", "Title", "Credits",
                "ContactHours", "HasTheory(Y/N)", "HasLab(Y/N)", "HasProject(Y/N)" };

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }

        // Sample Row
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("B.Tech");
        row.createCell(1).setCellValue("CSE");
        row.createCell(2).setCellValue(3);
        row.createCell(3).setCellValue("BCS301");
        row.createCell(4).setCellValue("Data Structures");
        row.createCell(5).setCellValue(4);
        row.createCell(6).setCellValue(45);
        row.createCell(7).setCellValue("Y");
        row.createCell(8).setCellValue("N");
        row.createCell(9).setCellValue("N");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ================= ASSIGNMENT TEMPLATE =================

    public byte[] generateAssignmentTemplate() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Assignment Template");

        Row header = sheet.createRow(0);
        String[] columns = { "CourseCode", "TeacherEmail", "Section", "AcademicYear", "IsSubjectHead(Y/N)" };

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }

        // Sample
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("BCS301");
        row.createCell(1).setCellValue("teacher@example.com");
        row.createCell(2).setCellValue("A");
        row.createCell(3).setCellValue("2024-25");
        row.createCell(4).setCellValue("N");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ================= IMPORT COURSES =================

    @Transactional
    public Map<String, Object> importCourses(MultipartFile file, Long departmentId) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        if (rows.hasNext())
            rows.next(); // Skip header

        while (rows.hasNext()) {
            Row row = rows.next();
            try {
                String progName = getCellValue(row.getCell(0));
                String branchName = getCellValue(row.getCell(1));
                String semNumStr = getCellValue(row.getCell(2));
                String code = getCellValue(row.getCell(3));
                String title = getCellValue(row.getCell(4));
                String creditsStr = getCellValue(row.getCell(5));
                String hoursStr = getCellValue(row.getCell(6));
                String hasTheory = getCellValue(row.getCell(7));
                String hasLab = getCellValue(row.getCell(8));
                String hasProject = getCellValue(row.getCell(9));

                if (code.isEmpty() || title.isEmpty())
                    continue;

                if (courseRepository.existsByCode(code)) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": Course code " + code + " already exists.");
                    failureCount++;
                    continue;
                }

                // Resolve Program
                Program prog = programRepository.findByNameAndDepartmentId(progName, departmentId)
                        .orElseThrow(
                                () -> new RuntimeException("Program '" + progName + "' not found in your department."));

                // Resolve Branch
                Branch branch = branchRepository.findByNameAndProgramId(branchName, prog.getId())
                        .orElseThrow(() -> new RuntimeException(
                                "Branch '" + branchName + "' not found in Program " + progName));

                // Resolve Semester
                int semNum = Integer.parseInt(semNumStr);
                Semester sem = semesterRepository
                        .findBySemesterNumberAndBranchIdAndProgramId(semNum, branch.getId(), prog.getId())
                        .orElseThrow(() -> new RuntimeException(
                                "Semester " + semNum + " not found for this Branch/Program."));

                Course course = new Course();
                course.setProgramId(prog.getId());
                course.setBranchId(branch.getId());
                course.setSemesterId(sem.getId());
                course.setCode(code);
                course.setTitle(title);
                course.setCredits(creditsStr.isEmpty() ? 0 : Integer.parseInt(creditsStr));
                course.setContactHour(hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr));
                course.setHasTheory("Y".equalsIgnoreCase(hasTheory));
                course.setHasLab("Y".equalsIgnoreCase(hasLab));
                course.setHasProject("Y".equalsIgnoreCase(hasProject));

                courseRepository.save(course);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
            }
        }
        workbook.close();

        Map<String, Object> res = new HashMap<>();
        res.put("successCount", successCount);
        res.put("failureCount", failureCount);
        res.put("errors", errors);
        return res;
    }

    // ================= IMPORT ASSIGNMENTS =================

    @Transactional
    public Map<String, Object> importAssignments(MultipartFile file, Long departmentId) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        Department dept = departmentRepository.findById(departmentId).orElse(null);

        if (rows.hasNext())
            rows.next();

        while (rows.hasNext()) {
            Row row = rows.next();
            try {
                String courseCode = getCellValue(row.getCell(0));
                String email = getCellValue(row.getCell(1));
                String section = getCellValue(row.getCell(2));
                String year = getCellValue(row.getCell(3));
                String isHead = getCellValue(row.getCell(4));

                Course course = courseRepository.findByCode(courseCode)
                        .orElseThrow(() -> new RuntimeException("Course code '" + courseCode + "' not found."));

                Teacher teacher = teacherRepository.findByEmailOfficial(email)
                        .orElseThrow(() -> new RuntimeException("Teacher with email '" + email + "' not found."));

                CourseTeacher assignment = new CourseTeacher();
                assignment.setCourse(course);
                assignment.setTeacher(teacher);
                assignment.setSection(section);
                assignment.setAcademicYear(year);
                assignment.setDepartment(dept);
                assignment.setIsSubjectHead("Y".equalsIgnoreCase(isHead));

                courseTeacherRepo.save(assignment);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
            }
        }
        workbook.close();

        Map<String, Object> res = new HashMap<>();
        res.put("successCount", successCount);
        res.put("failureCount", failureCount);
        res.put("errors", errors);
        return res;
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
