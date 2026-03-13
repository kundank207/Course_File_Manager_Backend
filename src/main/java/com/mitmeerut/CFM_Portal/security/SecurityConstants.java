package com.mitmeerut.CFM_Portal.security;

public class SecurityConstants {

    private SecurityConstants() {}

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String SECRET_KEY =
            "CHANGE_THIS_SECRET_KEY_MIN_32_CHARS_123456789";

    public static final long EXPIRATION_TIME =
            1000 * 60 * 60 * 24; // 24 hours
}
