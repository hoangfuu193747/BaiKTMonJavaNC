package com.qlktx.model;

public enum Role {
    STUDENT, ADMIN;

    public String getHomePath() {
        return this == ADMIN ? "/admin/home" : "/student/home";
    }
}
