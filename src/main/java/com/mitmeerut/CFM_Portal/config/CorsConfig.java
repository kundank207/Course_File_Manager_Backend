package com.mitmeerut.CFM_Portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class CorsConfig {

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration config = new CorsConfiguration();

                String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
                if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
                } else {
                        config.setAllowedOrigins(List.of(
                                        "http://localhost:5000",
                                        "http://localhost:5173"));
                }
                config.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("Authorization"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);

                return source;
        }
}