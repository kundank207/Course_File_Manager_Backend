package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "course_file_shares")
public class CourseFileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_file_id", nullable = false)
    private CourseFile courseFile;

    @ManyToOne
    @JoinColumn(name = "shared_by_id", nullable = false)
    private Teacher sharedBy;

    @ManyToOne
    @JoinColumn(name = "shared_with_id", nullable = false)
    private Teacher sharedWith;

    @Column(nullable = false)
    private LocalDateTime sharedAt;

    private String message;
}
