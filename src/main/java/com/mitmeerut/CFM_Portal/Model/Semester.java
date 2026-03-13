package com.mitmeerut.CFM_Portal.Model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;

@Entity
@Table(name = "Semester", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "semester_number", "branch_id" })
})
public class Semester {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer semester_number;
	private String label;

	@ManyToOne
	@JoinColumn(name = "program_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Program program;

	@ManyToOne
	@JoinColumn(name = "branch_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Branch branch;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getSemester_number() {
		return semester_number;
	}

	public void setSemester_number(Integer semester_number) {
		this.semester_number = semester_number;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Program getProgram() {
		return program;
	}

	public void setProgram(Program program) {
		this.program = program;
	}

	public Branch getBranch() {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

}
