package com.obsidiangate.mcpanel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WelcomeService {

    @Autowired
    private InitConfigService initConfigService;

    private final String MOJANG_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private final RestTemplate restTemplate = new RestTemplate();
    private final String limit = "26.1.3";

    public List<String> getAvailableVersions() {
        try {
            Map<String, Object> response = restTemplate.getForObject(MOJANG_MANIFEST_URL, Map.class);
            if (response == null || !response.containsKey("versions")) return List.of();

            List<Map<String, String>> allVersions = (List<Map<String, String>>) response.get("versions");

            return allVersions.stream()
                    .filter(v -> "release".equals(v.get("type"))) // Filter only release versions
                    .map(v -> v.get("id"))
                    .filter(this::isVersionLowerThanLimit) //
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of(limit);
        }
    }

    private boolean isVersionLowerThanLimit(String versionId) {
        return compareVersions(versionId, limit) < 0;
    }

    // Compares two version strings (e.g., "1.16.5" vs "1.17") and returns:
    // -1 if v1 < v2, 0 if v1 == v2, 1 if v1 > v2

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? tryParse(parts1[i]) : 0;
            int p2 = i < parts2.length ? tryParse(parts2[i]) : 0;
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        return 0;
    }

    private int tryParse(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setupServer(String version, String geminiApiKey, String token) {
        initConfigService.initConfig(version, geminiApiKey, token);
    }
}