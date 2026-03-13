package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.SystemSetting;
import com.mitmeerut.CFM_Portal.Repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    @Autowired
    private SystemSettingRepository repository;

    @Override
    public String getSetting(String key) {
        Optional<SystemSetting> setting = repository.findByKeyName(key);
        return setting.map(SystemSetting::getValue).orElse(null);
    }

    @Override
    public void updateSetting(String key, String value) {
        SystemSetting setting = repository.findByKeyName(key)
                .orElse(new SystemSetting(null, key, value, null));
        setting.setValue(value);
        repository.save(setting);
    }

    @Override
    public Map<String, String> getAllSettings() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getKeyName, SystemSetting::getValue));
    }

    @Override
    public void updateSettings(Map<String, String> settings) {
        settings.forEach(this::updateSetting);
    }
}
