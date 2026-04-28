package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.UserDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
                        .body(Map.of("message", "Error registering user: Username already exists or first setup completed"));
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

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PostMapping("/manager")
    public ResponseEntity<?> createManager(@RequestHeader("Authorization") String token, @RequestBody UserDTO userDTO) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        try {
            authService.createManager(userDTO, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "ok", "message", "Manager created successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String token, @PathVariable String username) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        try {
            authService.deleteManager(username, token);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

}
