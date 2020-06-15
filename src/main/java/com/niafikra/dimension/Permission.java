package com.niafikra.dimension;


import com.niafikra.dimension.core.security.Permissions;

@Permissions("dimension")
public class Permission {
    public static final String OVERRIDE_APPROVE_REQUEST = "ROLE_OVERRIDE_APPROVE_REQUEST";

    private Permission() {
    }
}
