package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.LogEntryDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.LogService;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @Autowired
    private AuthService authService;

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentLogs(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LogEntryType type) {

        if (!authService.authenticate(token) || !authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            List<LogEntryDTO> logs;
            if (type != null) {
                logs = logService.getLastLogsbyType(type);
            } else {
                logs = logService.getLastLogs();
            }
            return ResponseEntity.ok(logs);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid log type");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal error");
        }
    }
}