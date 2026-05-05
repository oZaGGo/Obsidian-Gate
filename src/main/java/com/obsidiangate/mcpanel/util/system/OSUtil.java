package com.obsidiangate.mcpanel.util.system;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class OSUtil {

    public static String getOSName() {
        return System.getProperty("os.name").toLowerCase();
    }

    public String executeCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        process.waitFor();
        return output.toString();
    }

}