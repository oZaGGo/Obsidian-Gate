package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.SystemMetricsDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import com.obsidiangate.mcpanel.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class HomeController {

    @Autowired
    AuthService authService;

    @Autowired
    private MetricsService metricsService;

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(@RequestHeader(value = "Authorization") String token) {

        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        SystemMetricsDTO metrics = metricsService.getSystemMetrics();

        return ResponseEntity.ok(metrics);
    }
}
