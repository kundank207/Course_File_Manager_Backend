package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Institute;
import com.mitmeerut.CFM_Portal.Repository.InstituteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InstituteServiceImpl implements InstituteService {

    @Autowired
    private InstituteRepository repo;

    @Override
    public List<Institute> getAll() {
        return repo.findAll();
    }

    @Override
    public Optional<Institute> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public Institute create(Institute inst) {
        return repo.save(inst);
    }

    @Override
    public Institute update(Long id, Institute updated) {
        Institute inst = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        inst.setName(updated.getName());
        inst.setCode(updated.getCode());
        inst.setAddress(updated.getAddress());
        inst.setEmail(updated.getEmail());
        inst.setPhone(updated.getPhone());
        inst.setWebsite(updated.getWebsite());

        return repo.save(inst);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
