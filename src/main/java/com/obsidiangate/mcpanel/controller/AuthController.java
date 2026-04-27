package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.UserDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        try {
            String token = authService.register(userDTO);

            if (token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Username already exists"));
            }

            return ResponseEntity.ok(Map.of("status", "ok", "token", token));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Internal error"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO userDTO) {

        String token = authService.login(userDTO);

        if (!token.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "ok", "token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid credentials"));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        if (authService.authenticate(token)) {
            return ResponseEntity.ok(Map.of("status", "valid"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "invalid"));
    }
}
