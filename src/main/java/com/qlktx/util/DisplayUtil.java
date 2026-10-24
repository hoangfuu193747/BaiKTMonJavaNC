package com.qlktx.util;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
public final class DisplayUtil {
    private DisplayUtil() { }
    public static String money(BigDecimal amount) {
        return amount == null ? "—" : NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }
}

