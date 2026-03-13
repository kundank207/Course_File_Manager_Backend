package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Branch;
import com.mitmeerut.CFM_Portal.Model.Program;
import com.mitmeerut.CFM_Portal.Model.Semester;
import com.mitmeerut.CFM_Portal.Repository.BranchRepository;
import com.mitmeerut.CFM_Portal.Repository.ProgramRepository;
import com.mitmeerut.CFM_Portal.Repository.SemesterRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SemesterServiceImpl implements SemesterService {

    @Autowired
    private SemesterRepository semesterRepo;

    @Autowired
    private ProgramRepository programRepo;

    @Autowired
    private BranchRepository branchRepo;

    @Override
    public List<Semester> getSemesters(Long programId, Long branchId) {
        Program p = programRepo.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));
        Branch b = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch Not Found"));
        return semesterRepo.findByProgramAndBranch(p, b);
    }

    @Override
    public Semester createSemester(Map<String, Object> body) {
        Integer semester_number = Integer.valueOf(body.get("semester_number").toString());
        Long programId = Long.valueOf(body.get("program_id").toString());
        Long branchId = Long.valueOf(body.get("branch_id").toString());

        if (semesterRepo.existsBySemesterNumberAndBranchIdAndProgramId(semester_number, branchId, programId)) {
            throw new RuntimeException("Semester " + semester_number + " already exists for this Program and Branch");
        }

        Semester s = new Semester();
        s.setLabel((String) body.get("label"));
        s.setSemester_number(semester_number);

        Program p = programRepo.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));
        s.setProgram(p);

        Branch b = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch Not Found"));
        s.setBranch(b);

        return semesterRepo.save(s);
    }

    @Override
    public Semester updateSemester(Long id, Map<String, Object> body) {
        Semester s = semesterRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Semester Not Found"));

        if (body.containsKey("semester_number") || body.containsKey("program_id") || body.containsKey("branch_id")) {
            Integer num = body.containsKey("semester_number")
                    ? Integer.valueOf(body.get("semester_number").toString())
                    : s.getSemester_number();
            Long pId = body.containsKey("program_id")
                    ? Long.valueOf(body.get("program_id").toString())
                    : s.getProgram().getId();
            Long bId = body.containsKey("branch_id")
                    ? Long.valueOf(body.get("branch_id").toString())
                    : s.getBranch().getId();

            if (!num.equals(s.getSemester_number()) || !pId.equals(s.getProgram().getId())
                    || !bId.equals(s.getBranch().getId())) {
                if (semesterRepo.existsBySemesterNumberAndBranchIdAndProgramId(num, bId, pId)) {
                    throw new RuntimeException("Semester " + num + " already exists for this Program and Branch");
                }
            }
        }

        if (body.containsKey("label"))
            s.setLabel((String) body.get("label"));

        if (body.containsKey("semester_number"))
            s.setSemester_number(Integer.valueOf(body.get("semester_number").toString()));

        if (body.containsKey("program_id")) {
            Program p = programRepo.findById(Long.valueOf(body.get("program_id").toString()))
                    .orElseThrow(() -> new RuntimeException("Program Not Found"));
            s.setProgram(p);
        }

        if (body.containsKey("branch_id")) {
            Branch b = branchRepo.findById(Long.valueOf(body.get("branch_id").toString()))
                    .orElseThrow(() -> new RuntimeException("Branch Not Found"));
            s.setBranch(b);
        }

        return semesterRepo.save(s);
    }

    @Override
    public void deleteSemester(Long id) {
        semesterRepo.deleteById(id);
    }

    @Override
    public List<Semester> getAllSemesters() {
        return semesterRepo.findAll();
    }

    @Override
    @Transactional
    public void generateSemesters(Long programId, Long branchId, int totalSemesters) {
        Program p = programRepo.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));
        Branch b = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch Not Found"));

        for (int i = 1; i <= totalSemesters; i++) {
            if (semesterRepo.existsBySemesterNumberAndBranchIdAndProgramId(i, branchId, programId)) {
                continue; // Skip if already exists
            }
            Semester s = new Semester();
            s.setProgram(p);
            s.setBranch(b);
            s.setSemester_number(i);
            s.setLabel("Semester " + i);
            semesterRepo.save(s);
        }
    }
}
