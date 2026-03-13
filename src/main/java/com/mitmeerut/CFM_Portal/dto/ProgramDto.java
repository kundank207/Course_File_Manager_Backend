package com.mitmeerut.CFM_Portal.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProgramDto {
    private Long id;
    @NotNull private Long departmentId;
    @NotBlank private String name;
    @NotBlank private String code;
    @NotNull private Integer durationYear;
    private String degreeType;
    // getters & setters
    // ... similar to above
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getDepartmentId(){return departmentId;} public void setDepartmentId(Long departmentId){this.departmentId=departmentId;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getCode(){return code;} public void setCode(String code){this.code=code;}
    public Integer getDurationYear(){return durationYear;} public void setDurationYear(Integer durationYear){this.durationYear=durationYear;}
    public String getDegreeType(){return degreeType;} public void setDegreeType(String degreeType){this.degreeType=degreeType;}
}

