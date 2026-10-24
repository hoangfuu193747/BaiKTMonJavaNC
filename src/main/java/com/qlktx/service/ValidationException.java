package com.qlktx.service;

import java.util.Map;

public class ValidationException extends Exception {
    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Invalid authentication form.");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() { return errors; }
}
