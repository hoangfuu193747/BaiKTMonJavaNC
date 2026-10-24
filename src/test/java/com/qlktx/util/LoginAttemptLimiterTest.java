package com.qlktx.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
    @Test void blocksRepeatedFailuresWithoutBlockingOtherAccountsAndResetsOnSuccess() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        String key = limiter.key("127.0.0.1", "student", "23110001");
        for (int attempt = 0; attempt < 8; attempt++) limiter.failed(key);
        assertTrue(limiter.blocked(key));
        assertFalse(limiter.blocked(limiter.key("127.0.0.1", "student", "23110002")));
        limiter.succeeded(key);
        assertFalse(limiter.blocked(key));
    }
}
