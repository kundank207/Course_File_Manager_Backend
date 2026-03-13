package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Department;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Model.User.userRole;
import com.mitmeerut.CFM_Portal.Repository.DepartmentRepository;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class BulkImportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.mitmeerut.CFM_Portal.Repository.InstituteRepository instituteRepository;

    @Autowired
    private EmailService emailService;

    public byte[] generateTemplate() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Faculty Import Template");

        // Header Row
        Row header = sheet.createRow(0);
        String[] columns = { "Name", "Email", "EmployeeCode", "Designation", "ContactNumber", "Department",
                "Role (TEACHER/HOD)" };

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

        // Add instructions or sample data
        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("John Doe");
        sample.createCell(1).setCellValue("john@example.com");
        sample.createCell(2).setCellValue("EMP001");
        sample.createCell(3).setCellValue("Professor");
        sample.createCell(4).setCellValue("9876543210");
        sample.createCell(5).setCellValue("Computer Science");
        sample.createCell(6).setCellValue("TEACHER");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Transactional
    public Map<String, Object> importFaculty(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        Set<String> processedEmails = new HashSet<>();

        // Skip header
        if (rows.hasNext())
            rows.next();

        while (rows.hasNext()) {
            Row row = rows.next();
            try {
                String name = getCellValue(row.getCell(0));
                String email = getCellValue(row.getCell(1));
                if (email != null)
                    email = email.toLowerCase().trim();

                String empCode = getCellValue(row.getCell(2));
                String designation = getCellValue(row.getCell(3));
                String contact = getCellValue(row.getCell(4));
                String deptName = getCellValue(row.getCell(5));
                String roleStr = getCellValue(row.getCell(6));

                if (email == null || email.isEmpty())
                    continue;

                // 1. Check if already processed in this Excel file
                if (processedEmails.contains(email)) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": Duplicate email " + email
                            + " found in this Excel file.");
                    failureCount++;
                    continue;
                }

                // 2. Check if already exists in Database
                if (userRepository.existsByEmail(email)) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": Email " + email + " already exists in system.");
                    failureCount++;
                    continue;
                }

                processedEmails.add(email);

                // Department Handling
                Department dept = departmentRepository.findByName(deptName)
                        .orElseGet(() -> {
                            if (deptName == null || deptName.trim().isEmpty())
                                return null;
                            Department d = new Department();
                            d.setName(deptName);
                            // Set a code (mandatory in model)
                            String generatedCode = deptName.replaceAll("\\s+", "").toUpperCase();
                            if (generatedCode.length() > 10)
                                generatedCode = generatedCode.substring(0, 10);
                            d.setCode(generatedCode + "_" + (int) (Math.random() * 999));

                            // Set Institute (mandatory in model)
                            instituteRepository.findAll().stream().findFirst().ifPresent(d::setInstitute);

                            return departmentRepository.save(d);
                        });

                if (dept == null) {
                    errors.add("Row " + row.getRowNum() + ": Department name missing.");
                    failureCount++;
                    continue;
                }

                if (dept.getInstitute() == null) {
                    errors.add(
                            "Row " + row.getRowNum() + ": No Institute found in system to assign to new department.");
                    failureCount++;
                    continue;
                }

                // Create Teacher
                Teacher teacher = new Teacher();
                teacher.setName(name);
                teacher.setEmailOfficial(email);
                teacher.setEmployeeCode(empCode);
                teacher.setDesignation(designation);
                teacher.setContactNumber(contact);
                teacher.setDepartment(dept);
                teacher.setIsActive(true); // Bulk import is pre-approved
                teacher = teacherRepository.save(teacher);

                // Create User
                User user = new User();
                user.setUsername(name); // or email if username must be unique
                user.setEmail(email);
                String tempPwd = generateRandomPassword();
                user.setPasswordHash(passwordEncoder.encode(tempPwd));

                userRole role = userRole.TEACHER;
                try {
                    if (roleStr != null)
                        role = userRole.valueOf(roleStr.toUpperCase());
                } catch (Exception e) {
                }

                user.setRole(role);
                user.setTeacher(teacher);
                user.setIsActive(true); // Pre-approved
                userRepository.save(user);

                // Send Email (Optional but recommended)
                try {
                    emailService.sendEmail(email, "Welcome to CFM Portal",
                            "Hello " + name + ",\n\nYour account has been created by the Administrator.\n" +
                                    "Login Email: " + email + "\n" +
                                    "Temporary Password: " + tempPwd + "\n\n" +
                                    "Please change your password after login.");
                } catch (Exception e) {
                    System.err.println("Failed to send welcome email to " + email);
                }

                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add("Row " + row.getRowNum() + ": Error - " + e.getMessage());
            }
        }

        workbook.close();

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("errors", errors);
        return result;
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell))
                    return cell.getDateCellValue().toString();
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
