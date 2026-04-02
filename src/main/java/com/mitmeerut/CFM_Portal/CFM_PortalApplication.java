package com.mitmeerut.CFM_Portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@org.springframework.cache.annotation.EnableCaching
public class CFM_PortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(CFM_PortalApplication.class, args);
	}

	@Bean
	public CommandLineRunner fixDatabase(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				// This drops the old unique index that didn't include revision_number
				jdbcTemplate.execute("ALTER TABLE course_file DROP INDEX UKpqtsho2peq6b9b2qe5fipdns2");
				System.out.println(
						"DATABASE REPAIR: Successfully dropped old unique constraint UKpqtsho2peq6b9b2qe5fipdns2");
			} catch (Exception e) {
				// Index might already be dropped or have a different name in different setups
				System.out.println(
						"DATABASE REPAIR: Old unique constraint check completed (already clean or not found).");
			}
		};
	}
}
