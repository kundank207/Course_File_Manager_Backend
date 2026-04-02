package com.mitmeerut.CFM_Portal.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "teacher", indexes = {
    @Index(name = "idx_teacher_dept", columnList = "department_id"),
    @Index(name = "idx_teacher_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional relation with Department (minimal)
    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonIgnore
    private Department department;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "employee_code", unique = true, length = 100)
    private String employeeCode;

    @Column(length = 100)
    private String designation;

    @Column(name = "email_official", length = 150)
    private String emailOfficial;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "is_active")
    private Boolean isActive = false; // signup se false, approval ke baad true

    @Column(name = "joined_on")
    private LocalDate joinedOn;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt = LocalDate.now();
}
