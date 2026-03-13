package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Course", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "title", "semester_id" })
})
@Data
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "program_id", nullable = false)
	private Long programId;

	@ManyToOne
	@JoinColumn(name = "program_id", insertable = false, updatable = false)
	private Program program;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "semester_id", nullable = false)
	private Long semesterId;

	@Column(length = 50, nullable = false)
	private String code;

	@Column(length = 200, nullable = false)
	private String title;

	private Integer credits;

	@Column(name = "contact_hour")
	private Integer contactHour;

	@Column(name = "has_theory", nullable = false)
	private Boolean hasTheory = true;

	@Column(name = "has_lab", nullable = false)
	private Boolean hasLab = false;

	@Column(name = "has_project", nullable = false)
	private Boolean hasProject = false;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProgramId() {
		return programId;
	}

	public void setProgramId(Long programId) {
		this.programId = programId;
	}

	public Long getBranchId() {
		return branchId;
	}

	public void setBranchId(Long branchId) {
		this.branchId = branchId;
	}

	public Long getSemesterId() {
		return semesterId;
	}

	public void setSemesterId(Long semesterId) {
		this.semesterId = semesterId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Integer getCredits() {
		return credits;
	}

	public void setCredits(Integer credits) {
		this.credits = credits;
	}

	public Integer getContactHour() {
		return contactHour;
	}

	public void setContactHour(Integer contactHour) {
		this.contactHour = contactHour;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Boolean getHasTheory() {
		return hasTheory;
	}

	public void setHasTheory(Boolean hasTheory) {
		this.hasTheory = hasTheory;
	}

	public Boolean getHasLab() {
		return hasLab;
	}

	public void setHasLab(Boolean hasLab) {
		this.hasLab = hasLab;
	}

	public Boolean getHasProject() {
		return hasProject;
	}

	public void setHasProject(Boolean hasProject) {
		this.hasProject = hasProject;
	}

	public Program getProgram() {
		return program;
	}

	public void setProgram(Program program) {
		this.program = program;
	}

	// getters & setters

}
