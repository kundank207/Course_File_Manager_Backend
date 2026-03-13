package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.mitmeerut.CFM_Portal.Model.Branch;
import com.mitmeerut.CFM_Portal.Model.Program;
import com.mitmeerut.CFM_Portal.Model.Semester;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByProgramAndBranch(Program program, Branch branch);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) > 0 FROM Semester s WHERE s.semester_number = :num AND s.branch.id = :branchId AND s.program.id = :programId")
    boolean existsBySemesterNumberAndBranchIdAndProgramId(
            @org.springframework.data.repository.query.Param("num") Integer num,
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("programId") Long programId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Semester s WHERE s.semester_number = :num AND s.branch.id = :branchId AND s.program.id = :programId")
    java.util.Optional<Semester> findBySemesterNumberAndBranchIdAndProgramId(
            @org.springframework.data.repository.query.Param("num") Integer num,
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("programId") Long programId);
}
