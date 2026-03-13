package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mitmeerut.CFM_Portal.Model.Institute;

@Repository
public interface InstituteRepository extends JpaRepository<Institute, Long> {
    // add custom queries if needed later
}
