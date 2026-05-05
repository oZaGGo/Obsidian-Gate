package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Value("${project.version:0.0.1-SNAPSHOT}")
    private String version;

    @Autowired
    private UpdateService updateService;

    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getVersion() {
        return ResponseEntity.ok(Map.of("version", version));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkUpdate() {
        boolean available = updateService.isUpdateAvailable();
        return ResponseEntity.ok(Map.of("updateAvailable", available));
    }

    @PostMapping("/update")
    public ResponseEntity<?> triggerUpdate(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token) || !authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Unauthorized"));
        }

        updateService.triggerSystemUpdate();

        return ResponseEntity.ok(Map.of(
                "message", "Update triggered. The system will restart shortly."
        ));
    }
}