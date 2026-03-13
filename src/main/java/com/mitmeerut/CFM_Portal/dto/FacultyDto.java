package com.mitmeerut.CFM_Portal.dto;

public class FacultyDto {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String designation;
    private boolean isActive;

    private Long departmentId;
    private String departmentName;
	public FacultyDto(Long id, String name, String email, String role, String designation, boolean isActive,
			Long departmentId, String departmentName) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.role = role;
		this.designation = designation;
		this.isActive = isActive;
		this.departmentId = departmentId;
		this.departmentName = departmentName;
	}
	public FacultyDto() {
		super();
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	public boolean isActive() {
		return isActive;
	}
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	public Long getDepartmentId() {
		return departmentId;
	}
	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}
	public String getDepartmentName() {
		return departmentName;
	}
	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}
	
	
    
    

 
 }

  
