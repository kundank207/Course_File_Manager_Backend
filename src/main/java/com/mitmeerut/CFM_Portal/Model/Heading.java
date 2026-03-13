package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "heading", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "title", "course_file_id", "parent_heading_id" })
}, indexes = {
        @Index(name = "idx_heading_coursefile", columnList = "course_file_id"),
        @Index(name = "idx_heading_parent", columnList = "parent_heading_id")
})
@Data
public class Heading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "course_file_id")
    private CourseFile courseFile;

    @ManyToOne
    @JoinColumn(name = "parent_heading_id")
    private Heading parentHeading;

    private String title;

    private Integer orderIndex;

    private LocalDateTime createdAt;
}
