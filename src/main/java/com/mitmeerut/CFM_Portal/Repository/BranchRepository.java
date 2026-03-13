package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.Branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByProgramId(Long programId);

    boolean existsByNameAndProgram_Id(String name, Long programId);

    java.util.Optional<Branch> findByNameAndProgramId(String name, Long programId);

}
