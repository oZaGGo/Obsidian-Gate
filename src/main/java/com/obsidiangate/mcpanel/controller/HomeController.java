package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.dto.SystemMetricsDTO;
import com.obsidiangate.mcpanel.service.AuthService;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

@RestController
@RequestMapping("/api/system")
public class HomeController {

    @Autowired
    AuthService authService;

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics(@RequestHeader(value = "Authorization") String token) {
        if (!authService.authenticate(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        SystemMetricsDTO dto = new SystemMetricsDTO();

        try {

            Process process = Runtime.getRuntime().exec("free -b");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Mem:")) {
                    // Split the line by white spaces
                    String[] parts = line.split("\\s+");

                    // 'free -b' structure:
                    // [0] Mem: [1] Total [2] Used [3] Free [4] Shared [5] Buff/Cache [6] Available
                    long totalBytes = Long.parseLong(parts[1]);
                    long availableBytes = Long.parseLong(parts[6]);
                    long usedBytes = totalBytes - availableBytes;

                    double totalGB = totalBytes / 1073741824.0;
                    double usedGB = usedBytes / 1073741824.0;

                    dto.setRamTotal(Math.round(totalGB * 100.0) / 100.0);
                    dto.setRamUsed(Math.round(usedGB * 100.0) / 100.0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpu = osBean.getSystemCpuLoad() * 100;
        dto.setCpuUsage(cpu < 0 ? 0 : Math.round(cpu * 100.0) / 100.0);

        return ResponseEntity.ok(dto);
    }
}
