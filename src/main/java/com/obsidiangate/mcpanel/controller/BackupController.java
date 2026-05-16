package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.dto.BackupDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.LogService;
import com.obsidiangate.mcpanel.service.WorldManagementService;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @Autowired
    private AppConfig appConfig;

    @PostMapping("/create")
    public ResponseEntity<?> createBackup(
            @RequestHeader("Authorization") String token,
            @RequestParam String alias,
            @RequestParam String name) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {

            if (worldManagementService.isBackupFinished()) {
                worldManagementService.createBackup(name, alias);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A backup is already in progress. Please wait until it finishes.");
            }

            logService.registerEntry(token, "Created Backup: " + name, LogEntryType.WORLDMANAGEMENT);

            return ResponseEntity.ok(Map.of("message", "Created Backup successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listBackups(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            List<BackupDTO> backups = worldManagementService.getAllBackupsSorted();
            return ResponseEntity.ok(backups);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching backups: " + e.getMessage());
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restoreBackup(
            @RequestHeader("Authorization") String token,
            @RequestParam String alias,
            @RequestParam String name) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            if (worldManagementService.isBackupFinished()) {
                worldManagementService.restoreBackup(alias, name);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A process is already in progress.");
            }

            logService.registerEntry(token, "Restored Backup: " + alias, LogEntryType.WORLDMANAGEMENT);
            return ResponseEntity.ok(Map.of("message", "Restore process started"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteBackup(
            @RequestHeader("Authorization") String token,
            @RequestParam String alias) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            worldManagementService.deleteBackup(alias);
            logService.registerEntry(token, "Deleted Backup: " + alias, LogEntryType.WORLDMANAGEMENT);
            return ResponseEntity.ok(Map.of("message", "Backup deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/schedule-interval")
    public ResponseEntity<?> getScheduleInterval(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        return ResponseEntity.ok(Map.of("interval", appConfig.getBackupTime()));
    }

    @PostMapping("/schedule-interval")
    public ResponseEntity<?> updateScheduleInterval(
            @RequestHeader("Authorization") String token,
            @RequestParam String interval) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            appConfig.setBackupTime(interval);

            logService.registerEntry(token, "Changed backup schedule to: " + interval, LogEntryType.WORLDMANAGEMENT);

            return ResponseEntity.ok(Map.of("message", "Schedule interval updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating interval: " + e.getMessage());
        }
    }
}
