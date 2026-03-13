package com.mitmeerut.CFM_Portal.dto;

import jakarta.validation.constraints.NotNull;

public class SemesterDto {
    private Long id;
    @NotNull private Long programId;
    @NotNull private Long branchId;
    @NotNull private Integer semesterNumber;
    private String label;
    // getters/setters
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getProgramId(){return programId;} public void setProgramId(Long programId){this.programId=programId;}
    public Long getBranchId(){return branchId;} public void setBranchId(Long branchId){this.branchId=branchId;}
    public Integer getSemesterNumber(){return semesterNumber;} public void setSemesterNumber(Integer semesterNumber){this.semesterNumber=semesterNumber;}
    public String getLabel(){return label;} public void setLabel(String label){this.label=label;}
}
