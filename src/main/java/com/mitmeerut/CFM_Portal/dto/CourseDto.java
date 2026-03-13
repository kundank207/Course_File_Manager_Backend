package com.mitmeerut.CFM_Portal.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CourseDto {
    private Long id;
    @NotNull private Long programId;
    @NotNull private Long branchId;
    @NotNull private Long semesterId;
    @NotBlank private String code;
    @NotBlank private String title;
    @NotNull private Integer credits;
    private Integer contactHour;
    // getters/setters
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getProgramId(){return programId;} public void setProgramId(Long programId){this.programId=programId;}
    public Long getBranchId(){return branchId;} public void setBranchId(Long branchId){this.branchId=branchId;}
    public Long getSemesterId(){return semesterId;} public void setSemesterId(Long semesterId){this.semesterId=semesterId;}
    public String getCode(){return code;} public void setCode(String code){this.code=code;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public Integer getCredits(){return credits;} public void setCredits(Integer credits){this.credits=credits;}
    public Integer getContactHour(){return contactHour;} public void setContactHour(Integer contactHour){this.contactHour=contactHour;}
}

