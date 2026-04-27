package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.ServerRuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
public class ServerRuntimeController {

    @Autowired
    private ServerRuntimeService serverService;

    @Autowired
    private AuthService authService;

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

        if (!serverService.isRunning()) {
            return ResponseEntity.badRequest().body("Server is offline");
        }

        serverService.sendCommand(command);
        return ResponseEntity.ok(Map.of("message", "Command sent"));
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<String> logs = serverService.getLogs();
        return ResponseEntity.ok(logs);
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