package com.mitmeerut.CFM_Portal;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "CFMadmin2026"; // CHANGE THIS
        String hash = encoder.encode(rawPassword);

        System.out.println("ADMIN PASSWORD HASH:");
        System.out.println(hash);
    }
}