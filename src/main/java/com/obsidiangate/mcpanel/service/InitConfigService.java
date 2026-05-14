package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.repository.UserAuthRepository;
import com.obsidiangate.mcpanel.util.listener.ApplicationReadyListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class InitConfigService {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ApplicationReadyListener applicationReadyListener;

    @Autowired
    private UserAuthRepository userAuthRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String MOJANG_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    private final String serverPath = Paths.get(System.getProperty("user.dir"), "mc_server").toString();
    private final String backupsPath = Paths.get(System.getProperty("user.dir"), "mc_backups").toString();

    public void initConfig(String version, String geminiApiKey, String token) {
        System.out.println("--- Init Config Service ---");
        if (!checkIfSetupCompleted(token)) {
            createStartFolders();
            downloadServerJar(version);
            saveConfig(geminiApiKey);
            completeSetup(token);
        } else {
            System.out.println("[INFO] Setup already completed. Skipping initialization.");
        }
    }

    private boolean checkIfSetupCompleted(String token) {
        var userOpt = userAuthRepository.findByToken(token);

        if (userOpt.isPresent()){
            var user = userOpt.get();
            return user.isSetupCompleted();
        } else {
            return false;
        }
    }

    // Necessary folders for the server and backups

    private void createStartFolders() {
        try {
            createDirectory(serverPath, "Server");
            createDirectory(backupsPath, "Backups");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to create necessary folders: " + e.getMessage());
        }
    }

    private void createDirectory(String pathStr, String label) throws IOException {
        Path path = Paths.get(pathStr);

        if (Files.notExists(path)) {
            Files.createDirectories(path);
            System.out.println("[OK] Folder for " + label + " created in: " + pathStr);
        } else {
            System.out.println("[INFO] Folder for " + label + " already exists.");
        }
    }

    // Downloads the server.jar for the specified version and saves it in the server folder

    private void downloadServerJar(String versionId) {
        try {
            Map<String, Object> manifest = restTemplate.getForObject(MOJANG_MANIFEST_URL, Map.class);
            List<Map<String, String>> versions = (List<Map<String, String>>) manifest.get("versions");

            String versionUrl = versions.stream()
                    .filter(v -> versionId.equals(v.get("id")))
                    .map(v -> v.get("url"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Version not found in Mojang manifest"));

            Map<String, Object> versionDetails = restTemplate.getForObject(versionUrl, Map.class);
            Map<String, Object> downloads = (Map<String, Object>) versionDetails.get("downloads");
            Map<String, String> serverDownload = (Map<String, String>) downloads.get("server");
            String downloadUrl = serverDownload.get("url");

            System.out.println("[INFO] Downloading server.jar for version " + versionId + "...");
            Path targetPath = Paths.get(serverPath, "server.jar");

            downloadFile(downloadUrl, targetPath);

            System.out.println("[SUCCESS] server.jar saved at: " + targetPath.toString());

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to download server.jar: " + e.getMessage());
        }
    }

    private void downloadFile(String urlStr, Path targetPath) throws IOException {
        URL url = new URL(urlStr);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    // Saves the Gemini API key in the application configuration

    private void saveConfig(String geminiApiKey) {
        appConfig.setAiToken(geminiApiKey);
        applicationReadyListener.saveConfig();
        System.out.println("[INFO] Gemini API key saved in configuration.");
    }

    // Marks the setup as completed in user data

    private void completeSetup( String token) {
        var userOpt = userAuthRepository.findByToken(token);
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            user.setSetupCompleted(true);
            userAuthRepository.save(user);
            System.out.println("[INFO] Setup marked as completed for user: " + user.getUsername());
        } else {
            System.err.println("[ERROR] User not found for token: " + token);
        }
    }
}
