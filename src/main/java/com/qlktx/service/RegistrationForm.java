package com.qlktx.service;

public record RegistrationForm(String studentId, String email, String phone,
        String birthDate, String password, String confirmPassword) { }
