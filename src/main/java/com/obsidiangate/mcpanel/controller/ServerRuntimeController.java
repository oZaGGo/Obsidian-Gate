package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.config.ServerConfig;
import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.LogService;
import com.obsidiangate.mcpanel.service.ServerRuntimeService;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
public class ServerRuntimeController {

    @Autowired
    private LogService logService;

    @Autowired
    private ServerRuntimeService serverService;

    @Autowired
    private AuthService authService;
    @Autowired
    private ServerConfig serverConfig;

    @PostMapping("/control")
    public ResponseEntity<?> controlServer(
            @RequestHeader("Authorization") String token,
            @RequestParam String action) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid Token");
        }

        try {
            switch (action.toLowerCase()) {
                case "start":
                    serverService.startServer();
                    break;
                case "stop":
                    serverService.stopServer();
                    break;
                case "restart":
                    serverService.restartServer();
                    break;
                default:
                    return ResponseEntity.badRequest().body("Error: Action '" + action + "' not recognized");
            }

            logService.registerEntry(token, "Executed server action: " + action, LogEntryType.SERVERRUNTIME);

            return ResponseEntity.ok(Map.of("message", "Action " + action + " executed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing server action: " + e.getMessage());
        }
    }

    @PostMapping("/command")
    public ResponseEntity<?> sendCommand(
            @RequestHeader("Authorization") String token,
            @RequestBody String command) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (!serverService.isRunning()) {
            return ResponseEntity.badRequest().body("Server is offline");
        }

        serverService.sendCommand(command);

        logService.registerEntry(token, "Sent command to server: " + command, LogEntryType.SERVERRUNTIME);

        return ResponseEntity.ok(Map.of("message", "Command sent"));
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<String> logs = serverService.getLogs();

        return ResponseEntity.ok(Map.of("logs", logs, "eula", serverConfig.isEula()));
    }

    @GetMapping("acceptEula")
    public ResponseEntity<?> acceptEula(@RequestHeader("Authorization") String token) throws IOException, InterruptedException {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        serverService.acceptEula();

        serverService.restartServer();

        return ResponseEntity.ok(Map.of("message", "EULA accepted"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        boolean isRunning = serverService.isRunning();
        String runtime = serverService.getUptime();

        return ResponseEntity.ok(Map.of(
                "status", isRunning ? "ONLINE" : "OFFLINE",
                "running", isRunning,
                "uptime", runtime
        ));
    }
}