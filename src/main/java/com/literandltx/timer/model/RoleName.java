package com.literandltx.timer.model;

import org.springframework.security.core.GrantedAuthority;

public enum RoleName implements GrantedAuthority {
    ADMIN, USER;

    public static final String ROLE = "ROLE_";

    @Override
    public String getAuthority() {
        return ROLE + name();
    }

}
