package com.qlktx.util;

public final class CsvUtil {
    private CsvUtil() { }
    public static String cell(Object value) {
        String text=value==null?"":value.toString();
        String leading=text.stripLeading();
        if(!leading.isEmpty()&&"=+-@".indexOf(leading.charAt(0))>=0)text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
}
