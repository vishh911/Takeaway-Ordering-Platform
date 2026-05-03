package com.campuseateries.model;

public enum UserRole {
    STUDENT,
    ADMIN;

    public static UserRole valueOfInsensitive(String raw) {
        return UserRole.valueOf(raw.trim().toUpperCase());
    }

    public static UserRole parse(String sqlValue) {
        return UserRole.valueOfInsensitive(sqlValue);
    }
}
