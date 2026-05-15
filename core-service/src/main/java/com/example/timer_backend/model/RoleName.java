package com.example.timer_backend.model;

import org.springframework.security.core.GrantedAuthority;

public enum RoleName implements GrantedAuthority {
    ADMIN, USER, USER_PREMIUM;

    public static final String ROLE = "ROLE_";

    @Override
    public String getAuthority() {
        return ROLE + name();
    }
}
