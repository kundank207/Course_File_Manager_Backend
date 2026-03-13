package com.mitmeerut.CFM_Portal.config;

import com.mitmeerut.CFM_Portal.security.SecurityConstants;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Key;

@Configuration
public class JwtConfig {

    @Bean
    public Key jwtSigningKey() {
        return Keys.hmacShaKeyFor(
                SecurityConstants.SECRET_KEY.getBytes()
        );
    }
}