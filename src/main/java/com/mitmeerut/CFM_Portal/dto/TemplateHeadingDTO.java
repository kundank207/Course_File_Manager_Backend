package com.mitmeerut.CFM_Portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateHeadingDTO {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Object structure;

    @NotNull
    private Object checklist;
}
