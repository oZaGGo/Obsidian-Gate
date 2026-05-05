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
            osUtil.executeCommand("git fetch");

            // Last commit id
            String local = osUtil.executeCommand("git rev-parse HEAD").trim();

            // Last commit id of the remote branch
            String remote = osUtil.executeCommand("git rev-parse @{u}").trim();

            // If local and remote commit ids are different, there is an update available
            return !local.equals(remote);
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