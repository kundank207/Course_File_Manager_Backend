package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Program;

import java.util.List;
import java.util.Map;

public interface ProgramService {

    List<Program> getAllPrograms();

    Program createProgram(Map<String, Object> body);

    Program updateProgram(Long id, Map<String, Object> body);

    void deleteProgram(Long id);
}
