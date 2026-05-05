package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.util.system.OSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class UpdateService {

    @Autowired
    private OSUtil osUtil;

    public boolean isUpdateAvailable() {
        try {
            // Check remote branches
            osUtil.executeCommand("git fetch");
            // Check local status against remote
            String status = osUtil.executeCommand("git status -uno");

            // If status is "is behind", origin contain new changes
            return status.contains("is behind");
        } catch (Exception e) {
            return false;
        }
    }

    public void triggerSystemUpdate() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                System.exit(10); // This is for the deployment script
            } catch (Exception e) {
                System.exit(1);
            }
        }).start();
    }
}