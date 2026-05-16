package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.UserDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(3)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket getBucket(String ip) {
        return cache.computeIfAbsent(ip, k -> createNewBucket());
    }

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
    public ResponseEntity<?> login(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        String ip = getClientIP(request);
        Bucket bucket = getBucket(ip);

        if (bucket.tryConsume(1)) {
            String token = authService.login(userDTO);
            if (!token.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "ok", "token", token));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many attempts from your IP. Try again in 3 minutes."));
        }
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
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String token, @PathVariable("username") String username) {
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

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String token, @RequestBody UserDTO user) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        try {
            authService.changePassword(user, token);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Password change successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Boolean>> isAdmin(@RequestHeader("Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().body(Map.of("admin", authService.isAdmin(token)));
    }

    @GetMapping("/setup")
    public ResponseEntity<Map<String, Boolean>> setup(@RequestParam("token") String token) {
        return ResponseEntity.ok().body(Map.of("setupCompleted", authService.userSetupCompleted(token)));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

}
