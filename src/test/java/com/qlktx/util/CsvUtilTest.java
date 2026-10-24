package com.qlktx.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {
    @Test void quotesCommasNewlinesAndQuotesArePreserved() {
        assertEquals("\"Khu A, \"\"Nam\"\"\nTầng 1\"",CsvUtil.cell("Khu A, \"Nam\"\nTầng 1"));
        assertEquals("\"\"",CsvUtil.cell(null));
    }
    @Test void spreadsheetFormulaPrefixesAreNeutralized() {
        for(String value:new String[]{"=1+1"," +SUM(A1)","-1+2","@SUM(1)","\t=1+1","\n=1+1"})
            assertTrue(CsvUtil.cell(value).startsWith("\"'"));
        assertEquals("\"700000\"",CsvUtil.cell(700000));
    }
}
