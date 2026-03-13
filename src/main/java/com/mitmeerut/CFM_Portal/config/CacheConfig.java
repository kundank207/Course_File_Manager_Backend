package com.mitmeerut.CFM_Portal.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        // Define caches for frequently used but seldom changed data
        cacheManager.setCacheNames(Arrays.asList(
                "departments",
                "programs",
                "courses",
                "teachers",
                "academic_years",
                "course_structure"));
        return cacheManager;
    }
}
