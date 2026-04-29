package com.obsidiangate.mcpanel.util.system;

public class OSUtil {

    public static String getOSName() {
        return System.getProperty("os.name").toLowerCase();
    }

}