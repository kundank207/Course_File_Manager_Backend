package com.mitmeerut.CFM_Portal.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BranchDto {
    private Long id;
    @NotNull private Long programId;
    @NotBlank private String name;
    private String code;
    // getters/setters
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getProgramId(){return programId;} public void setProgramId(Long programId){this.programId=programId;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getCode(){return code;} public void setCode(String code){this.code=code;}
}

