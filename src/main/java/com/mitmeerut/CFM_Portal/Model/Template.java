package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Template")
public class Template {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "department_id", nullable = false)
	private Long departmentId;

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(columnDefinition = "json")
	private String structure; // JSON string

	@Column(columnDefinition = "json")
	private String checklist; // JSON string

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "template_type", nullable = false)
	private TemplateType templateType = TemplateType.THEORY;

	@Column(name = "sections_count")
	private Integer sectionsCount = 0;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStructure() {
		return structure;
	}

	public void setStructure(String structure) {
		this.structure = structure;
	}

	public String getChecklist() {
		return checklist;
	}

	public void setChecklist(String checklist) {
		this.checklist = checklist;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public TemplateType getTemplateType() {
		return templateType;
	}

	public void setTemplateType(TemplateType templateType) {
		this.templateType = templateType;
	}

	public Integer getSectionsCount() {
		return sectionsCount;
	}

	public void setSectionsCount(Integer sectionsCount) {
		this.sectionsCount = sectionsCount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Template(Long id, Long departmentId, String name, String description, String structure, String checklist,
			Long createdBy, TemplateType templateType, Integer sectionsCount, LocalDateTime createdAt) {
		super();
		this.id = id;
		this.departmentId = departmentId;
		this.name = name;
		this.description = description;
		this.structure = structure;
		this.checklist = checklist;
		this.createdBy = createdBy;
		this.templateType = templateType;
		this.sectionsCount = sectionsCount;
		this.createdAt = createdAt;
	}

	public Template() {
		super();
	}

}
