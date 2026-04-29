package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.ServerConfigDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.LogService;
import com.obsidiangate.mcpanel.service.ServerConfigService;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ServerConfigController {

    @Autowired
    private LogService logService;

    @Autowired
    private ServerConfigService configService;

    @Autowired
    private AuthService authService;

    @GetMapping("/get")
    public ResponseEntity<?> getConfig(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        return ResponseEntity.ok(configService.getConfig());
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveConfig(
            @RequestHeader("Authorization") String token,
            @RequestBody ServerConfigDTO dto) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        configService.updateConfig(dto);

        logService.registerEntry(token, "Updated configuration: " + dto.toString(), LogEntryType.SERVERCONFIG);

        return ResponseEntity.ok(Map.of("message", "Configuration successfully updated"));
    }

    @GetMapping("/icon")
    public ResponseEntity<Resource> getIcon() {
        Resource resource = configService.getIconResource();
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"server-icon.png\"")
                .body(resource);
    }

    @PostMapping("/icon/upload")
    public ResponseEntity<?> uploadIcon(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            configService.saveIcon(file);

            logService.registerEntry(token, "Uploaded new server icon", LogEntryType.SERVERCONFIG);

            return ResponseEntity.ok(Map.of("message", "Icon uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading icon: " + e.getMessage());
        }
    }
}