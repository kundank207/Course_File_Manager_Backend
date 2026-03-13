package com.mitmeerut.CFM_Portal.Service;

import java.util.Map;

public interface SystemSettingService {
    String getSetting(String key);

    void updateSetting(String key, String value);

    Map<String, String> getAllSettings();

    void updateSettings(Map<String, String> settings);
}
