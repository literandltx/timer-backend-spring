package com.literandltx.timer.security;

public final class SecurityConstants {

    // Prevent instantiation
    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String BEARER_PREFIX = "Bearer ";
    
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

}
