package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.dto.SystemMetricsDTO;
import com.obsidiangate.mcpanel.model.UserCoords;
import com.obsidiangate.mcpanel.model.World;
import com.obsidiangate.mcpanel.repository.UserCoordsRepository;
import com.obsidiangate.mcpanel.repository.WorldRepository;
import com.obsidiangate.mcpanel.util.ai.AIChat;
import com.obsidiangate.mcpanel.util.enumerator.ChatCommandType;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private UserCoordsRepository userCoordsRepository;

    @Autowired
    @Lazy
    private WorldManagementService worldManagementService;

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
            case COORDS -> {
                String[] parts = args.split(" ", 2);
                String subCommand = (parts.length > 0) ? parts[0].toLowerCase() : "";
                String extraArgs = (parts.length > 1) ? parts[1] : "";

                World currentWorld = worldRepository.findCurrentWorld().orElse(null);

                if (currentWorld == null) {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"[Error] Cannot find active world.\",\"color\":\"red\"}");
                    return;
                }

                switch (subCommand) {
                    case "new" -> handleNewCoord(playerName, extraArgs, currentWorld, runtimeService);
                    case "del" -> handleDelCoord(playerName, extraArgs, currentWorld, runtimeService);
                    case "list" -> handleListCoords(playerName, currentWorld, runtimeService);
                    default -> runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .coords <new | del | list>\",\"color\":\"red\"}");
                }
            }
            case BACKUP -> {
                String backupAlias = args.trim();

                if (backupAlias.isEmpty()) {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .backup [alias]\",\"color\":\"red\"}");
                    return;
                }

                World currentWorld = worldRepository.findCurrentWorld().orElse(null);

                if (currentWorld == null) {
                    runtimeService.sendCommand("tellraw " + playerName + " {\"text\":\"[Error] Cannot find active world.\",\"color\":\"red\"}");
                    return;
                }else {
                    logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .backup", LogEntryType.MINECRAFT);
                    runtimeService.sendCommand("say [Live] Backup process started by " + playerName);
                }

                worldManagementService.createBackup(currentWorld.getName(), backupAlias);
            }
        }
    }

    // Sub process for handling loop commands like .info and .tps without blocking main thread

    private void startSubProcess(ServerRuntimeService service) {
        new Thread(() -> {
            while (true) {
                try {
                    // Only if server running
                    if (service.isRunning()) {
                        userMetricsInfo(service);
                        tpsInfo(service);
                    }
                    Thread.sleep(3000); // Every 3 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    // Metrics info for players with active broadcast, every 3 seconds, in format: [Metrics] CPU: 15.5% | RAM: 3.2/8.0 GB

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

    // Tps info for players with active tps calculation, every 3 seconds, in format: [Metrics] TPS: 19.95 (PERFECT) with color green, yellow or red based on tps value

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

    // Handlers for .coords subcommands

    private void handleNewCoord(String playerName, String alias, World world, ServerRuntimeService runtime) {
        if (alias.isEmpty()) {
            runtime.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .coords new [alias]\",\"color\":\"red\"}");
            return;
        }

        logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .coords new", LogEntryType.MINECRAFT);

        CompletableFuture<String> dimFuture = CompletableFuture.supplyAsync(() -> fetchPlayerDimension(playerName, runtime));
        CompletableFuture<String> posFuture = CompletableFuture.supplyAsync(() -> fetchPlayerPosition(playerName, runtime));

        CompletableFuture.allOf(dimFuture, posFuture).thenAccept(v -> {
            String dim = dimFuture.join();
            String pos = posFuture.join();

            if (dim != null && pos != null) {
                UserCoords coord = UserCoords.builder()
                        .playerName(playerName)
                        .alias(alias)
                        .coordinates(pos)
                        .dimension(dim)
                        .world(world)
                        .build();

                userCoordsRepository.save(coord);
                runtime.sendCommand(String.format("tellraw %s {\"text\":\"[Coords] '%s' saved en %s.\",\"color\":\"green\"}", playerName, alias, dim));
            } else {
                runtime.sendCommand(String.format("tellraw %s {\"text\":\"[Error] Timed out fetching data (Dim: %s, Pos: %s)\",\"color\":\"red\"}",
                        playerName, (dim != null ? "OK" : "NULL"), (pos != null ? "OK" : "NULL")));
            }
        }).exceptionally(ex -> {
            runtime.sendCommand("tellraw " + playerName + " {\"text\":\"[Error] Internal async error.\",\"color\":\"red\"}");
            return null;
        });
    }

    private void handleDelCoord(String playerName, String alias, World world, ServerRuntimeService runtime) {
        if (alias.isEmpty()) {
            runtime.sendCommand("tellraw " + playerName + " {\"text\":\"Usage: .coords del [alias]\",\"color\":\"red\"}");
            return;
        }
        logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .coords del", LogEntryType.MINECRAFT);
        userCoordsRepository.deleteByPlayerNameAndWorldAndAlias(playerName, world, alias);
        runtime.sendCommand(String.format("tellraw %s {\"text\":\"[Coords] Deleted: %s\",\"color\":\"yellow\"}", playerName, alias));
    }

    private void handleListCoords(String playerName, World world, ServerRuntimeService runtime) {
        String currentDim = fetchPlayerDimension(playerName, runtime);
        List<UserCoords> coords = userCoordsRepository.findByPlayerNameAndWorldAndDimension(playerName, world, currentDim);

        if (coords.isEmpty()) {
            runtime.sendCommand(String.format("tellraw %s {\"text\":\"Coords not registered.\",\"color\":\"gray\"}", playerName));
        } else {
            logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " used .coords list", LogEntryType.MINECRAFT);
            runtime.sendCommand(String.format("tellraw %s {\"text\":\"--- %s (%s) ---\",\"color\":\"aqua\"}", playerName, currentDim, world.getName()));
            for (UserCoords c : coords) {
                String alias = c.getAlias();
                String coordStr = c.getCoordinates();

                String tellrawJson = String.format(
                        "[\"\",{\"text\":\"• %s: \",\"color\":\"white\"}," +
                                "{\"text\":\"%s\",\"color\":\"yellow\",\"underlined\":true," +
                                "\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"%s\"}}]",
                        alias, coordStr, coordStr
                );

                runtime.sendCommand("tellraw " + playerName + " " + tellrawJson);
                runtime.sendCommand("tellraw " + playerName + " "); // Empty line for spacing
            }
        }
    }

    // Helpers for fetching player dimension and position using Minecraft commands

    private String fetchPlayerDimension(String playerName, ServerRuntimeService runtime) {
        String res = runtime.sendCommandWithResponse("data get entity " + playerName + " Dimension", "data: \"minecraft:", 3000);
        return (res != null) ? parseDimension(res) : null;
    }

    private String fetchPlayerPosition(String playerName, ServerRuntimeService runtime) {
        String res = runtime.sendCommandWithResponse("data get entity " + playerName + " Pos", "data: [", 3000);
        return (res != null) ? parseCoords(res) : null;
    }

    // Parsers

    private String parseDimension(String logLine) {
        try {
            if (logLine.contains("overworld")) return "OVERWORLD";
            if (logLine.contains("the_nether")) return "NETHER";
            if (logLine.contains("the_end")) return "END";

            String data = logLine.split("data: ")[1].replace("\"", "").trim();
            if (data.contains(":")) return data.split(":")[1].toUpperCase();
            return data.toUpperCase();
        } catch (Exception e) {
            return "OVERWORLD";
        }
    }

    private String parseCoords(String logLine) {
        try {
            int startIndex = logLine.indexOf("data: [");
            if (startIndex == -1) return null;

            String rawData = logLine.substring(startIndex + 6, logLine.indexOf("]", startIndex) + 1);
            String cleanData = rawData.replace("[", "").replace("]", "");
            String[] parts = cleanData.split(",");

            double x = Double.parseDouble(parts[0].replaceAll("[^0-9.-]", "").trim());
            double y = Double.parseDouble(parts[1].replaceAll("[^0-9.-]", "").trim());
            double z = Double.parseDouble(parts[2].replaceAll("[^0-9.-]", "").trim());

            return String.format("X: %.1f, Y: %.1f, Z: %.1f", x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

}