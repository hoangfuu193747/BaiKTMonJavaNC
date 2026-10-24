package com.qlktx.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.servlet.http.HttpSession;

public final class CsrfUtil {
    private static final String KEY = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() { }

    public static String token(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(KEY);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(KEY, token);
            }
            return token;
        }
    }

    public static boolean valid(HttpSession session, String supplied) {
        if (session == null || supplied == null || supplied.length() > 128) return false;
        Object stored = session.getAttribute(KEY);
        return stored instanceof String token && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
