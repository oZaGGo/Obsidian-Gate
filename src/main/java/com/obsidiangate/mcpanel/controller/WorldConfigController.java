package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.WorldDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.WorldConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/world")
public class WorldConfigController {

    @Autowired
    private WorldConfigService worldService;

    @Autowired
    private AuthService authService;

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentWorld(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        WorldDTO current = worldService.getCurrentWorld();

        return ResponseEntity.ok(current);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllWorlds(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        return ResponseEntity.ok(worldService.getAllWorlds());
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveWorld(
            @RequestHeader("Authorization") String token,
            @RequestBody WorldDTO dto) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            worldService.saveWorld(dto);
            return ResponseEntity.ok(Map.of("message", "World configuration saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving world: " + e.getMessage());
        }
    }

    @PostMapping("/set-active")
    public ResponseEntity<?> setActiveWorld(
            @RequestHeader("Authorization") String token,
            @RequestParam String name) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            worldService.setActiveWorld(name);
            return ResponseEntity.ok(Map.of("message", "Active world changed to: " + name));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error changing active world: " + e.getMessage());
        }
    }
}