package com.mitmeerut.CFM_Portal.dto;

public class DepartmentDTO {

    private String name;
    private String code;
    private Long instituteId;
    private Long hodId; // optional

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getInstituteId() { return instituteId; }
    public void setInstituteId(Long instituteId) { this.instituteId = instituteId; }

    public Long getHodId() { return hodId; }
    public void setHodId(Long hodId) { this.hodId = hodId; }
}