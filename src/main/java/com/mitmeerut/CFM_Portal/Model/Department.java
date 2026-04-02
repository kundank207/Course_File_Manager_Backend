package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
@Data
@Entity
@Table(name = "Department", indexes = {
    @Index(name = "idx_dept_institute", columnList = "institute_id"),
    @Index(name = "idx_dept_hod", columnList = "hod_id")
})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institute_id", nullable = false)
    private Institute institute;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "hod_id", nullable = true)
    private Teacher hod;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;


}
