package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Program;
import com.mitmeerut.CFM_Portal.Service.ProgramService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/program")

public class ProgramController {

    @Autowired
    private ProgramService programService;

    @GetMapping
    public List<Program> getAllPrograms() {
        return programService.getAllPrograms();
    }

    @PostMapping
    public Program createProgram(@RequestBody Map<String, Object> body) {
        return programService.createProgram(body);
    }

    @PutMapping("/{id}")
    public Program updateProgram(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return programService.updateProgram(id, body);
    }

    @DeleteMapping("/{id}")
    public String deleteProgram(@PathVariable("id") Long id) {
        programService.deleteProgram(id);
        return "Program Deleted";
    }
}
