package com.qlktx.util;
/** Generates the isolated HTTP fixture hash; not packaged in the production WAR. */
public final class PortalHashTool {
    public static void main(String[] args) { System.out.print(BCryptPasswordUtil.hash("Portal-test-2026!")); }
}
