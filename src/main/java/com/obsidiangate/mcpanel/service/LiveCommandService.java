package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.dto.SystemMetricsDTO;
import com.obsidiangate.mcpanel.util.ai.AIChat;
import com.obsidiangate.mcpanel.util.enumerator.ChatCommandType;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveCommandService {

    @Autowired
    private LogService logService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private AIChat aiChat;

    // Safe threads
    private final Map<String, Boolean> userMetricsBroadcastStatus = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userTpsCalcStatus = new ConcurrentHashMap<>();

    private boolean subProcessRunning = false;

    public void handleLeftPlayer(String player){
        userMetricsBroadcastStatus.forEach((playerName, active) -> {
            if (player.equals(playerName)) {
                userMetricsBroadcastStatus.put(playerName, false);
            }
        });
        userTpsCalcStatus.forEach((playerName, active) -> {
            if (player.equals(playerName)) {
                userTpsCalcStatus.put(playerName, false);
            }
        });
    }

    public void execute(ChatCommandType type, String playerName, String args, ServerRuntimeService runtimeService) {
        switch (type) {
            case ECHO -> {
                if (args.isEmpty()) {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .echo <message>\",\"color\":\"red\"}");
                } else {
                    runtimeService.sendCommand("say [" + playerName + "] " + args);
                    logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .echo", LogEntryType.MINECRAFT);
                }
            }
            case INFO -> {

                if (!subProcessRunning){
                    startSubProcess(runtimeService);
                    subProcessRunning = true;
                }

                if (args.equalsIgnoreCase("start")) {
                    runtimeService.sendCommand("say [Live] Metrics broadcast started for " + playerName);
                    userMetricsBroadcastStatus.put(playerName, true);
                    logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .info start", LogEntryType.MINECRAFT);
                } else if (args.equalsIgnoreCase("stop")) {
                    runtimeService.sendCommand("say [Live] Metrics broadcast stopped for " + playerName);
                    userMetricsBroadcastStatus.remove(playerName);
                    logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .info start", LogEntryType.MINECRAFT);
                } else {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .info <start> | <stop>\",\"color\":\"red\"}");
                }
            }
            case ASK -> {
                if (args.isEmpty()) {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .ask <question>\",\"color\":\"red\"}");
                } else {
                    logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .ask", LogEntryType.MINECRAFT);
                    runtimeService.sendCommand("say [Live] Ai is thinking... ");
                    new Thread(() -> {
                        try {
                            String aiResponse = aiChat.askAI(args);

                            System.out.println(aiResponse);

                            if (aiResponse == null || aiResponse.isEmpty()) {
                                aiResponse = "I couldn't think of anything to say...";
                            }

                            String escapedResponse = aiResponse
                                    .replace("\"", "'")
                                    .replace("\n", " ");

                            runtimeService.sendCommand("tellraw @a [" +
                                    "{\"text\":\"[AI] \",\"color\":\"dark_purple\",\"bold\":true}," +
                                    "{\"text\":\"" + escapedResponse + "\",\"color\":\"light_purple\"}" +
                                    "]");

                        } catch (Exception e) {
                            runtimeService.sendCommand("say [Live] Unexpected error occurred while processing AI response.");
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
            case TPS -> {
                if (!subProcessRunning){
                    startSubProcess(runtimeService);
                    subProcessRunning = true;
                }

                if (args.equalsIgnoreCase("start")) {
                    runtimeService.sendCommand("say [Live] Tps calculation started by " + playerName);
                    userTpsCalcStatus.put(playerName, true);
                    logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .tps start", LogEntryType.MINECRAFT);
                } else if (args.equalsIgnoreCase("stop")) {
                    runtimeService.sendCommand("say [Live] Tps calculation stoped by " + playerName);
                    userTpsCalcStatus.remove(playerName);
                    logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .tps stop", LogEntryType.MINECRAFT);
                } else {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .tps <start> | <stop>\",\"color\":\"red\"}");
                }
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
                        tpsInfo(service);
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

    private void tpsInfo(ServerRuntimeService service) {
        userTpsCalcStatus.forEach((playerName, active) -> {
            if (active) {
                double tps = calculateTPS(service);

                String color;
                String status;

                // Scale
                if (tps >= 19.9) {
                    color = "green";
                    status = "PERFECT";
                } else if (tps >= 18.5) {
                    color = "yellow";
                    status = "GOOD";
                } else {
                    color = "red";
                    status = "LAG";
                }

                String tpsJson = String.format(
                        "[\"\",{\"text\":\"[Metrics] \",\"color\":\"gray\"}," +
                                "{\"text\":\"TPS: %.2f \",\"color\":\"%s\"}," +
                                "{\"text\":\"(%s)\",\"color\":\"%s\",\"bold\":true}]",
                        tps, color, status, color
                );
                service.sendCommand("tellraw " + playerName + " " + tpsJson);
            }
        });
    }

    private double calculateTPS(ServerRuntimeService service) {
        // First ticks
        String res1 = service.sendCommandWithResponse("time query gametime", "is", 2000);
        if (res1 == null) return 0.0;

        long t1 = parseGameTicks(res1);
        long realT1 = System.currentTimeMillis();

        // Wait 1s
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Second ticks
        String res2 = service.sendCommandWithResponse("time query gametime", "is", 2000);
        if (res2 == null) return 0.0;

        long t2 = parseGameTicks(res2);
        long realT2 = System.currentTimeMillis();

        // (Delta Ticks / Delta Real time) * 1000 ms
        double tps = (double) (t2 - t1) / (realT2 - realT1) * 1000.0;

        // Limit TPS to 20 and round to 2 decimals
        return Math.round(Math.min(20.0, tps) * 100.0) / 100.0;
    }

    // Line example: [12:10:18] [Server thread/INFO]: The game time is 90424 tick(s)
    private long parseGameTicks(String logLine) {
        try {
            String[] parts = logLine.split("is ");
            if (parts.length > 1) {
                // Second part (" 90424 tick(s)")
                // Delete all non numeric characters and parse to long
                return Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            System.err.println("Error parsin ticks: " + e.getMessage());
        }
        return 0;
    }
}