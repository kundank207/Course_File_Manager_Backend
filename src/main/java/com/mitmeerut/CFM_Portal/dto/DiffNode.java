package com.mitmeerut.CFM_Portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffNode {
    private Long id;
    private String title;
    private String nodeType; // HEADING, DOCUMENT
    private String diffStatus; // ADDED, REMOVED, MODIFIED, UNCHANGED
    private String oldValue;
    private String newValue;
    private List<DiffNode> children;
    private List<DiffNode> documents;
}
