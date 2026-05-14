package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.PropertiesConfig;
import com.obsidiangate.mcpanel.config.ServerConfig;
import com.obsidiangate.mcpanel.model.World;
import com.obsidiangate.mcpanel.repository.WorldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Properties;

@Service
public class PropertiesService {

    @Autowired
    private PropertiesConfig propertiesConfig;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private WorldRepository worldRepository;

    private final String propertiesPath = Paths.get(System.getProperty("user.dir"), "mc_server", "server.properties").toString();
    private final String eulaPath = Paths.get(System.getProperty("user.dir"), "mc_server", "eula.txt").toString();

    public void acceptEula() {
        Properties eulaProps = new Properties();
        File eulaFile = new File(eulaPath);

        if (eulaFile.exists()) {
            try (InputStream in = new FileInputStream(eulaFile)) {
                eulaProps.load(in);
            } catch (IOException e) {
                System.err.println("Warning: Could not read eula.txt.");
            }
        }

        eulaProps.setProperty("eula", "true");

        try (OutputStream out = new FileOutputStream(eulaFile)) {
            eulaProps.store(out, "By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/mc-eula).");
            System.out.println("[OK] EULA has been accepted in: " + eulaPath);
        } catch (IOException e) {
            throw new RuntimeException("Error writing to eula.txt", e);
        }
    }

    public void updateProperties() {
        propertiesConfig.setMotd(serverConfig.getDescription());
        propertiesConfig.setMaxPlayers(String.valueOf(serverConfig.getMaxPlayers()));
        propertiesConfig.setViewDistance(String.valueOf(serverConfig.getRenderDistance()));
        propertiesConfig.setSimulationDistance(String.valueOf(serverConfig.getSimulationDistance()));
        propertiesConfig.setServerPort(String.valueOf(serverConfig.getServerPort()));


        World worldConfig = worldRepository.findCurrentWorld().orElse(null);
        if (worldConfig != null) {
            propertiesConfig.setLevelName(worldConfig.getName());
            propertiesConfig.setLevelSeed(worldConfig.getSeed());
            propertiesConfig.setDifficulty(worldConfig.getDifficulty().toLowerCase());
            propertiesConfig.setGamemode(worldConfig.getGamemode().toLowerCase());
            propertiesConfig.setHardcore(String.valueOf(worldConfig.isHardcore()));
        }

    }

    public void saveToFile() {
        Properties props = new Properties();
        File file = new File(propertiesPath);

        // Load current file to preserve any properties not managed by the panel
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Error loading existing server.properties", e);
            }
        }

        // Mappin properties class fields to file
        // Convert CamelCase (viewDistance) to kebab-case (view-distance)
        for (Field field : propertiesConfig.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(propertiesConfig);
                if (value != null) {
                    // to kebab-case
                    String key = field.getName().replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
                    props.setProperty(key, value.toString());
                }
            } catch (IllegalAccessException e) {
                System.err.println("Could not access field: " + field.getName());
            }
        }

        // Save file changes
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Managed by ObsidianGate Panel");
        } catch (IOException e) {
            throw new RuntimeException("Error writing to server.properties", e);
        }
    }
}