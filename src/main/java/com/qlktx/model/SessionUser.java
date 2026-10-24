package com.qlktx.model;

import java.io.Serializable;

/** Session identity contains no password or database credentials. */
public final class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String username;
    private final String displayName;
    private final String email;
    private final Role role;

    public SessionUser(String id, String username, String displayName, String email, Role role) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getHomePath() { return role.getHomePath(); }
    public boolean isAdmin() { return role == Role.ADMIN; }
}
