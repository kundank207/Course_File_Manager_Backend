package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "Approval")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Approval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_file_id")
    private CourseFile courseFile;

    @ManyToOne
    @JoinColumn(name = "approver_id")
    private Teacher approver;

    private String stage;
    private String status;

    @Lob
    private String comment;

    @Column(name = "acted_at")
    private LocalDateTime actedAt = LocalDateTime.now();
}
