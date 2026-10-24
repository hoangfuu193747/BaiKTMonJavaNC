package com.qlktx.util;

import com.qlktx.dao.QuanTriVienDAO;
import java.io.Console;
import java.util.Arrays;

/** Local CLI only. There is no public endpoint for creating an administrator. */
public final class AdminSetup {
    private AdminSetup() { }

    public static void main(String[] args) throws Exception {
        String username = args.length > 0 ? args[0] : "admin";
        if (!username.matches("[A-Za-z0-9_.-]{1,50}")) throw new IllegalArgumentException("Invalid administrator username.");
        boolean generate = args.length > 1 && "--generate".equals(args[1]);
        String password;
        if (generate) {
            password = BCryptPasswordUtil.randomPassword();
        } else {
            Console console = System.console();
            if (console == null) throw new IllegalStateException("Run this command in an interactive terminal, or use --generate.");
            char[] first = console.readPassword("Admin password (8-128 characters): ");
            char[] second = console.readPassword("Confirm admin password: ");
            try {
                if (first == null || second == null || !Arrays.equals(first, second) || first.length < 8 || first.length > 128) {
                    throw new IllegalArgumentException("Passwords must match and contain 8-128 characters.");
                }
                password = new String(first);
            } finally {
                if (first != null) Arrays.fill(first, '\0');
                if (second != null) Arrays.fill(second, '\0');
            }
        }
        if(!BCryptPasswordUtil.withinLimit(password))throw new IllegalArgumentException("Password exceeds 72 UTF-8 bytes.");
        boolean created = new QuanTriVienDAO().createIfMissing(username, "Quản trị viên", BCryptPasswordUtil.hash(password));
        if (!created) {
            System.out.println("Administrator already exists; nothing was changed.");
            return;
        }
        System.out.println("Administrator created: " + username);
        if (generate) System.out.println("Initial administrator password: " + password);
    }
}
