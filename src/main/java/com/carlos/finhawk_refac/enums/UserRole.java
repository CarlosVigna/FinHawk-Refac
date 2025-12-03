package com.carlos.finhawk_refac.enums;

public enum UserRole {
    ADMIN("ADMIN"),
    VIEWER("VIEWER");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}