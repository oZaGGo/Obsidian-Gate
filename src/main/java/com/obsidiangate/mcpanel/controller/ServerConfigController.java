package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.config.ServerConfig;
import com.obsidiangate.mcpanel.dto.ServerConfigDTO;
import com.obsidiangate.mcpanel.model.ServerConfigModel;
import com.obsidiangate.mcpanel.repository.ServerConfigRepository;
import com.obsidiangate.mcpanel.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ServerConfigController {

    @Autowired
    private ServerConfigRepository repository;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private AuthService authService;

    private final String ICON_PATH = System.getProperty("user.dir") + "/mc_server/server-icon.png";


    @GetMapping("/get")
    public ResponseEntity<?> getConfig(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        return ResponseEntity.ok(repository.findActiveConfig());
    }


    @PostMapping("/save")
    public ResponseEntity<?> saveConfig(
            @RequestHeader("Authorization") String token,
            @RequestBody ServerConfigDTO dto) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        ServerConfigModel current = repository.findActiveConfig();
        if (current != null) {
            current.setName(dto.getName());
            current.setDescription(dto.getDescription());
            current.setMaxGbRam(dto.getMaxGbRam());
            current.setRenderDistance(dto.getRenderDistance());
            current.setSimulationDistance(dto.getSimulationDistance());
            current.setMaxPlayers(dto.getMaxPlayers());
            current.setRconPort(dto.getRconPort());
        }

        repository.save(current);
        serverConfig.refresh();

        return ResponseEntity.ok(Map.of("message", "Configuration successfully updated"));
    }


    @GetMapping("/icon")
    public ResponseEntity<Resource> getIcon() {
        File file = new File(ICON_PATH);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
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
            File dest = new File(ICON_PATH);
            if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();

            file.transferTo(dest);
            return ResponseEntity.ok(Map.of("message", "Icon uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading icon: " + e.getMessage());
        }
    }
}