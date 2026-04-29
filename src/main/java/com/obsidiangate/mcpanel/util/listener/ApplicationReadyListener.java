package com.obsidiangate.mcpanel.util.listener;

import com.obsidiangate.mcpanel.config.AppConfig;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ApplicationReadyListener {

    private final Environment environment;

    private final String CONFIG_FILE = "server-config.json";

    private final AppConfig appConfig;

    private final ObjectMapper objectMapper;

    public ApplicationReadyListener(Environment environment, AppConfig appConfig, ObjectMapper objectMapper) {
        this.environment = environment;
        this.appConfig = appConfig;
        this.objectMapper = new ObjectMapper().rebuild()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println();
        loadOrSaveConfig();
        printStartupBanner();
        System.out.println();
    }

    private void loadOrSaveConfig() {
        File file = new File(CONFIG_FILE);
        try {
            if (file.exists()) {
                AppConfig loadedConfig = objectMapper.readValue(file, AppConfig.class);

                appConfig.setAuthTimeMins(loadedConfig.getAuthTimeMins());
                appConfig.setFirstSetup(loadedConfig.isFirstSetup());
                appConfig.setAiToken(loadedConfig.getAiToken());

                System.out.println("[Config] server-config.json loaded successfully.");
            } else {
                System.out.println("[Config] server-config.json not found. Creating default...");
                if (file.createNewFile()) {
                    System.out.println("[Config] Blank file created successfully.");
                }
            }

            objectMapper.writeValue(file, appConfig);

        } catch (JacksonException e) {
            System.err.println("[Config] Critical error handling server-config.json: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[Config] IO error handling server-config.json: " + e.getMessage());
        }

    }

    public void printStartupBanner() {
        String port = environment.getProperty("local.server.port");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String host;

        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "localhost";
        }

        System.out.println("\n----------------------------------------------------------");
        System.out.println("   ¡ObsidianGate started succesful!");
        System.out.println("   Local:    http://localhost:" + port + contextPath);
        System.out.println("   Red:      http://" + host + ":" + port + contextPath);
        System.out.println("----------------------------------------------------------\n");
    }
}