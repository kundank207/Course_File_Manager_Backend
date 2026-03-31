package com.mitmeerut.CFM_Portal.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mitmeerut.CFM_Portal.Model.Branch;
import com.mitmeerut.CFM_Portal.Service.BranchService;

@RestController
@RequestMapping("/api/branch")

public class BranchController {

    @Autowired
    private BranchService branchService;

    @GetMapping
    public List<Branch> getBranches(@RequestParam("programId") Long programId) {
        return branchService.getBranchesByProgram(programId);
    }

    @GetMapping("/all")
    public List<Branch> getAllBranches() {
        return branchService.getAllBranches();
    }

    @PostMapping
    public Branch createBranch(@RequestBody com.mitmeerut.CFM_Portal.dto.BranchDto dto) {
        return branchService.createBranch(dto);
    }

    @PutMapping("/{id}")
    public Branch updateBranch(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return branchService.updateBranch(id, body);
    }

    @DeleteMapping("/{id}")
    public String deleteBranch(@PathVariable("id") Long id) {
        branchService.deleteBranch(id);
        return "Branch Deleted";
    }
}
