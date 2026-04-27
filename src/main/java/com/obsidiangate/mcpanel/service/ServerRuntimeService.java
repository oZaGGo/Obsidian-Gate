package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServerRuntimeService {

    @Autowired
    private ServerConfig serverConfig;

    private Process serverProcess;
    private final List<String> consoleLogs = new ArrayList<>();

    private final String serverPath = Paths.get(System.getProperty("user.dir"), "mc_server").toString();
    private final String jarName = "server.jar";

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
                "-jar",
                jarName,
                "nogui"
        );

        pb.directory(new File(serverPath));
        pb.redirectErrorStream(true);

        this.serverProcess = pb.start();

        //Console loggin

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (consoleLogs) {
                        consoleLogs.add(line);
                        if (consoleLogs.size() > 100) consoleLogs.remove(0);
                    }
                }
            } catch (IOException e) {
                System.err.println("Console closed: " + e.getMessage());
            }
        }).start();
    }

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
                writer.newLine(); // IMPORTANTE: Sin el salto de línea, el server no sabe que el comando terminó
                writer.flush();
            } catch (IOException e) {
                System.err.println("Error al enviar comando: " + e.getMessage());
            }
        }
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
}