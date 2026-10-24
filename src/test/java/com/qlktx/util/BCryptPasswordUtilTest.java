package com.qlktx.util;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class BCryptPasswordUtilTest {
    @Test void bcryptFitsDatabaseAndRejectsWrongPassword(){
        String hash=BCryptPasswordUtil.hash("Mat-khau-2026");
        assertEquals(60,hash.length());
        assertTrue(BCryptPasswordUtil.verify("Mat-khau-2026",hash));
        assertFalse(BCryptPasswordUtil.verify("wrong",hash));
        assertFalse(BCryptPasswordUtil.verify("123","123"));
    }
    @Test void limitsUtf8BytesInsteadOfTruncatingUnicode(){
        assertTrue(BCryptPasswordUtil.withinLimit("a".repeat(72)));
        assertFalse(BCryptPasswordUtil.withinLimit("a".repeat(73)));
        assertFalse(BCryptPasswordUtil.withinLimit("ế".repeat(25)));
        assertThrows(IllegalArgumentException.class,()->BCryptPasswordUtil.hash("ế".repeat(25)));
    }
}
