package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.InitConfigDTO;
import com.obsidiangate.mcpanel.service.WelcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/welcome")
public class WelcomeController {

    @Autowired
    private WelcomeService welcomeService;

    @GetMapping("/versions")
    public List<String> getVersions() {
        return welcomeService.getAvailableVersions();
    }

    @PostMapping("/start-config")
    public ResponseEntity<?> startConfig(@RequestHeader("Authorization") String token, @RequestBody InitConfigDTO configDTO) {

        System.out.println("--- Init config Controller ---");
        System.out.println("Version: " + configDTO.getVersion());
        System.out.println("API Key Gemini: " + (configDTO.getGeminiApiKey().isEmpty() ? "Not present" : "Present"));

        welcomeService.setupServer(configDTO.getVersion(), configDTO.getGeminiApiKey(), token);

        return ResponseEntity.ok(Map.of(
                "status", "success"
        ));
    }
}