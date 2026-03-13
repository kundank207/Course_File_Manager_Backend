package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Institute;
import java.util.List;
import java.util.Optional;

public interface InstituteService {

    List<Institute> getAll();
    Optional<Institute> findById(Long id);
    Institute create(Institute institute);
    Institute update(Long id, Institute institute);
    void delete(Long id);
}