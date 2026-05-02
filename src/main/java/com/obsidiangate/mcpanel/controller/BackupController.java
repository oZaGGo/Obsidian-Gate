package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.LogService;
import com.obsidiangate.mcpanel.service.WorldManagementService;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@CrossOrigin(origins = "*")
public class BackupController {

    @Autowired
    private AuthService authService;

    @Autowired
    private WorldManagementService worldManagementService;

    @Autowired
    private LogService logService;

    @PostMapping("/create")
    public ResponseEntity<?> createBackup(
            @RequestHeader("Authorization") String token,
            @RequestParam String alias,
            @RequestParam String name) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            worldManagementService.createBackup(name, alias);

            logService.registerEntry(token, "Created Backup: " + name, LogEntryType.WORLDMANAGEMENT);

            return ResponseEntity.ok(Map.of("message", "Created Backup successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
