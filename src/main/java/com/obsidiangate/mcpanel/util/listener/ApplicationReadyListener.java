package com.obsidiangate.mcpanel.util.listener;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.model.UserAuth;
import com.obsidiangate.mcpanel.model.World;
import com.obsidiangate.mcpanel.repository.UserAuthRepository;
import com.obsidiangate.mcpanel.repository.WorldRepository;
import com.obsidiangate.mcpanel.service.WorldConfigService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;

@Component
public class ApplicationReadyListener {

    private final Environment environment;

    private final String CONFIG_FILE = "server-config.json";

    private final AppConfig appConfig;

    private final ObjectMapper objectMapper;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private WorldConfigService worldConfigService;

    public ApplicationReadyListener(Environment environment, AppConfig appConfig, ObjectMapper objectMapper) {
        this.environment = environment;
        this.appConfig = appConfig;
        this.objectMapper = new ObjectMapper().rebuild()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        loadOrCreateConfig();

        //First setup
        if (appConfig.isFirstSetup()) {
            System.out.println("\n[First Setup] Detected first setup. Please configure your server by editing the server-config.json file.");
            System.out.println();
            System.out.println("\n[First Setup] Creating SYSTEM user...");
            createSystemUser();
            System.out.println("\n[First Setup] Creating default world...");
            //createDefaultWorld();
            appConfig.setFirstSetup(false);
            // Save initial config
            saveConfig();
        }else {
            System.out.println("\n[Startup] Loading configuration...");
            loadOrCreateConfig();
        }

        System.out.println();
        printStartupBanner();
        System.out.println();
    }


    private void createDefaultWorld(){
        World world = new World();

        world.setName("world");
        world.setDifficulty("normal");
        world.setGamemode("survival");
        world.setHardcore(false);
        world.setCurrent(true);

        worldRepository.save(world);

        worldConfigService.setActiveWorld("world");

        System.out.println("\n[First Setup] Default world created.");
    }

    private void createSystemUser(){
        UserAuth systemUser = new UserAuth();
        systemUser.setUsername("SYSTEM");
        systemUser.setToken(UUID.randomUUID().toString());
        systemUser.setPassword(UUID.randomUUID().toString());
        systemUser.setAdmin(false);
        userAuthRepository.save(systemUser);
        appConfig.setSystemUsrToken(systemUser.getToken());
        System.out.println("\n[First Setup] SYSTEM user created with token: " + systemUser.getToken());
    }

    private void loadOrCreateConfig() {
        File file = new File(CONFIG_FILE);
        try {
            if (file.exists()) {
                AppConfig loadedConfig = objectMapper.readValue(file, AppConfig.class);

                appConfig.setAuthTimeMins(loadedConfig.getAuthTimeMins());
                appConfig.setFirstSetup(loadedConfig.isFirstSetup());
                appConfig.setAiToken(loadedConfig.getAiToken());
                appConfig.setSystemUsrToken(loadedConfig.getSystemUsrToken());
                appConfig.setBackupTime(loadedConfig.getBackupTime());
                appConfig.setServerLimitVersion(loadedConfig.getServerLimitVersion());

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

    @PreDestroy
    public void saveConfig(){
        File file = new File(CONFIG_FILE);
        try {
            if (file.exists()) {
                objectMapper.writeValue(file, appConfig);
                System.out.println("[Config] server-config.json saved successfully.");
            }
        } catch (JacksonException e) {
            System.err.println("[Config] Critical error handling server-config.json: " + e.getMessage());
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