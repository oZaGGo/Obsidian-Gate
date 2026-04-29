package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.dto.SystemMetricsDTO;
import com.obsidiangate.mcpanel.util.enumerator.ChatCommandType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveCommandService {

    @Autowired
    private MetricsService metricsService;

    // Safe threads
    private final Map<String, Boolean> userMetricsBroadcastStatus = new ConcurrentHashMap<>();

    private boolean metricsThreadRunning = false;

    public void execute(ChatCommandType type, String playerName, String args, ServerRuntimeService runtimeService) {
        switch (type) {
            case ECHO -> {
                if (args.isEmpty()) {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .echo <message>\",\"color\":\"red\"}");
                } else {
                    runtimeService.sendCommand("say [" + playerName + "] " + args);
                }
            }
            case INFO -> {

                if (!metricsThreadRunning){
                    startSubProcess(runtimeService);
                    metricsThreadRunning = true;
                }

                if (args.equalsIgnoreCase("start")) {
                    runtimeService.sendCommand("say [Live] Metrics broadcast started for " + playerName);
                    userMetricsBroadcastStatus.put(playerName, true);
                } else if (args.equalsIgnoreCase("stop")) {
                    runtimeService.sendCommand("say [Live] Metrics broadcast stopped for " + playerName);
                    userMetricsBroadcastStatus.remove(playerName); // Mejor remove que false para limpiar el mapa
                } else {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .info <start> | <stop>\",\"color\":\"red\"}");
                }
            }
            case ASK -> {
                // TODO
            }
        }
    }

    private void startSubProcess(ServerRuntimeService service) {
        new Thread(() -> {
            while (true) {
                try {
                    // Only if server running
                    if (service.isRunning()) {
                        userMetricsInfo(service);
                    }
                    Thread.sleep(3000); // Every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void userMetricsInfo(ServerRuntimeService service) {
        SystemMetricsDTO metrics = metricsService.getSystemMetrics();

        double cpu = metrics.getCpuUsage();
        double ram = metrics.getRamUsed();
        double ramTotal = metrics.getRamTotal();

        String metricsMsg = String.format(
                "tellraw @a {\"text\":\"[Metrics] CPU: %.1f%% | RAM: %.1f/%.1f GB\",\"color\":\"gold\"}",
                cpu, ram, ramTotal
        );

        userMetricsBroadcastStatus.forEach((playerName, active) -> {
            if (active) {
                service.sendCommand(metricsMsg.replace("@a", playerName));
            }
        });
    }
}