package com.mitmeerut.CFM_Portal.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dmdgurwmv");
        config.put("api_key", "926841668861555");
        config.put("api_secret", "xhW331xskDZDtBP2Q3Kh1QyIlj0");
        config.put("secure", "true");
        return new Cloudinary(config);
    }
}
