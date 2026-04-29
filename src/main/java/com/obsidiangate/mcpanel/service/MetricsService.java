package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.dto.SystemMetricsDTO;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

@Service
public class MetricsService {

    public SystemMetricsDTO getSystemMetrics() {
        SystemMetricsDTO dto = new SystemMetricsDTO();

        try {
            Process process = Runtime.getRuntime().exec("free -b");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpu = osBean.getCpuLoad() * 100;
        dto.setCpuUsage(cpu < 0 ? 0 : Math.round(cpu * 100.0) / 100.0);

        return dto;
    }
}