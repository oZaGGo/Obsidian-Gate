package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.config.ServerConfig;
import com.obsidiangate.mcpanel.util.enumerator.ChatCommandType;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ServerRuntimeService {

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private LogService logService;

    @Autowired
    private LiveCommandService liveCommandService;

    private Process serverProcess;
    private final List<String> consoleLogs = new ArrayList<>();

    private final String serverPath = Paths.get(System.getProperty("user.dir"), "mc_server").toString();

    private final String jarName = "server.jar";

    private static final Pattern CHAT_PATTERN = Pattern.compile("<(\\w+)> (.*)");

    private long startTime = 0;

    private volatile long totalLinesAdded = 0;

    private final List<String> logBuffer = Collections.synchronizedList(new ArrayList<>());

    public void startServer() throws IOException {
        if (serverProcess != null && serverProcess.isAlive()) {
            return;
        }

        String ramFlag = "-Xmx" + serverConfig.getMaxGbRam() + "G";
        String startRamFlag = "-Xms" + serverConfig.getMaxGbRam() + "G";

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                ramFlag,
                startRamFlag,
                // Skip warnings
                "--enable-native-access=ALL-UNNAMED",
                "--add-modules=jdk.crypto.ec",
                "-Dsun.stdout.encoding=UTF-8",
                "-jar",
                jarName,
                "nogui"
        );

        pb.directory(new File(serverPath));
        pb.redirectErrorStream(true);

        this.serverProcess = pb.start();
        this.startTime = System.currentTimeMillis();

        //Console loggin

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {

                    if (line != null) {
                        synchronized (logBuffer) {
                            logBuffer.add(line);
                            totalLinesAdded++;
                            if (logBuffer.size() > 500) logBuffer.remove(0);
                        }
                    }

                    detectPlayerJoin(line);
                    detectPlayerQuit(line);

                    String cleanLine = sanitizeLogLine(line);

                    synchronized (consoleLogs) {
                        consoleLogs.add(cleanLine);
                        if (consoleLogs.size() > 100) consoleLogs.remove(0);
                    }

                    handleChatCommands(line);
                }
            } catch (IOException e) {
                System.err.println("Console closed: " + e.getMessage());
            }
        }).start();
    }


    private String sanitizeLogLine(String line) {

        String projectRoot = System.getProperty("user.dir");
        if (line.contains(projectRoot)) {
            return line.replace(projectRoot, "[PROJECT_ROOT]");
        }

        if (line.contains("zagg294")) {
            return line.replaceAll("/home/zagg294/[^\\s]*", "[PROTECTED_PATH]");
        }

        return line;
    }

    @PreDestroy
    public void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));
                writer.write("stop");
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                serverProcess.destroy();
            }
        }
    }

    public void sendCommand(String command) {
        if (serverProcess != null && serverProcess.isAlive()) {
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));
                writer.write(command);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("Error sending command: " + e.getMessage());
            }
        }
    }

    public String sendCommandWithResponse(String command, String expectedKeyword, long timeoutMillis) {
        if (!isRunning()) return null;

        long startLineCount = totalLinesAdded; // Cuántas líneas había antes de enviar
        sendCommand(command);

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            long currentLineCount = totalLinesAdded;

            if (currentLineCount > startLineCount) {
                int linesToCheck = (int) (currentLineCount - startLineCount);

                synchronized (logBuffer) {
                    int size = logBuffer.size();
                    int startIdx = Math.max(0, size - linesToCheck);

                    for (int i = startIdx; i < size; i++) {
                        String line = logBuffer.get(i);
                        if (line.contains(expectedKeyword)) {
                            return line;
                        }
                    }
                }
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null; // Timeout
    }

    public void restartServer() throws IOException, InterruptedException {
        stopServer();
        if (serverProcess != null) {
            serverProcess.waitFor();
        }
        startServer();
    }

    public List<String> getLogs() {
        synchronized (consoleLogs) {
            return new ArrayList<>(consoleLogs);
        }
    }

    public boolean isRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }

    public String getUptime() {
        if (serverProcess == null || !serverProcess.isAlive()) {
            this.startTime = 0;
            return "00:00:00";
        }

        long now = System.currentTimeMillis();
        long diff = now - this.startTime;

        long seconds = (diff / 1000) % 60;
        long minutes = (diff / (1000 * 60)) % 60;
        long hours = (diff / (1000 * 60 * 60));

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void handleChatCommands(String line) {
        Matcher matcher = CHAT_PATTERN.matcher(line);

        if (matcher.find()) {
            String playerName = matcher.group(1);
            String fullMessage = matcher.group(2).trim();

            if (!fullMessage.startsWith(".")) {
                return;
            }

            String lowerMessage = fullMessage.toLowerCase();

            for (ChatCommandType command : ChatCommandType.values()) {
                // Command prefix is "."
                String cmdWithPrefix = "." + command.getValue();

                if (lowerMessage.startsWith(cmdWithPrefix)) {

                    // Extract args after command
                    String args = "";
                    if (fullMessage.length() > cmdWithPrefix.length()) {
                        args = fullMessage.substring(cmdWithPrefix.length()).trim();
                    }

                    // "this" is used to maintain the process reference and service access for command execution

                    final String finalArgs = args;
                    new Thread(() -> {
                        liveCommandService.execute(command, playerName, finalArgs, this);
                    }).start();

                    break;
                }
            }
        }
    }

    private void detectPlayerJoin(String line) {
        if (line.contains("joined the game")) {
            try {
                String playerName = line.split("INFO]: ")[1].split(" joined")[0].trim();
                logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " joined the game", LogEntryType.MINECRAFT);
            } catch (Exception e) {
                System.err.println("Error parsing join message: " + e.getMessage());
            }
        }
    }


    private void detectPlayerQuit(String line) {
        if (line.contains("left the game")) {
            try {
                String playerName = line.split("INFO]: ")[1].split(" left")[0].trim();
                logService.registerEntry(appConfig.getSystemUsrToken(), "Player " + playerName + " left the game", LogEntryType.MINECRAFT);

                // Handle command interruptions
                liveCommandService.handleLeftPlayer(playerName);

            } catch (Exception e) {
                System.err.println("Error parsing quit message: " + e.getMessage());
            }
        }
    }
}