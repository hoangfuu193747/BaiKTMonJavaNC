package com.qlktx.model;

/** DAO result used only while authenticating; never put this in an HTTP session. */
public record AuthAccount(SessionUser user, String passwordHash, boolean active) { }
