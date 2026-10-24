package com.qlktx.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
    private RegistrationForm valid() {
        return new RegistrationForm("23110001", "23110001@school.edu.vn", "0912345678", "2005-01-01", "Strong-pass-2026", "Strong-pass-2026");
    }

    @Test void acceptsValidFormAndOptionalPhone() {
        assertTrue(AuthService.validateRegistration(valid()).isEmpty());
        RegistrationForm form = valid();
        assertTrue(AuthService.validateRegistration(new RegistrationForm(form.studentId(), form.email(), "", form.birthDate(), form.password(), form.confirmPassword())).isEmpty());
    }

    @Test void rejectsInvalidIdentifiersEmailsPasswordsAndConfirmation() {
        var errors = AuthService.validateRegistration(new RegistrationForm("' OR 1=1--", "invalid-email", "abc", "wrong-date", "short", "other"));
        assertTrue(errors.keySet().containsAll(java.util.Set.of("studentId", "email", "phone", "password", "confirmPassword")));
    }

    @Test void rejectsOversizedValues() {
        RegistrationForm form = valid();
        var errors = AuthService.validateRegistration(new RegistrationForm("x".repeat(11), "a".repeat(121) + "@school.edu.vn", "", "", "x".repeat(73), "x".repeat(73)));
        assertTrue(errors.containsKey("studentId"));
        assertTrue(errors.containsKey("email"));
        assertTrue(errors.containsKey("password"));
    }
}
