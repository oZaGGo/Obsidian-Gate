package com.obsidiangate.mcpanel.config;

import com.obsidiangate.mcpanel.model.ServerConfigModel;
import com.obsidiangate.mcpanel.repository.ServerConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Data
public class ServerConfig {

    @Autowired
    private ServerConfigRepository serverConfigRepository;

    private boolean firstSetup;
    private int maxGbRam;
    private String name;
    private String description;
    private int renderDistance;
    private int simulationDistance;
    private int maxPlayers = 20;
    private int rconPort = 25575;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        ServerConfigModel model = serverConfigRepository.findActiveConfig();

        if (model != null) {
            this.firstSetup = model.isFirstSetup();
            this.maxGbRam = model.getMaxGbRam();
            this.name = model.getName();
            this.description = model.getDescription();
            this.renderDistance = model.getRenderDistance();
            this.simulationDistance = model.getSimulationDistance();
            this.maxPlayers = model.getMaxPlayers();
            this.rconPort = model.getRconPort();
        } else {
            // If bd is empty
            this.firstSetup = true;
            this.maxGbRam = 2;
            this.name = "ObsidianGate Server";
            this.description = "Server managed by ObsidianGate";
            this.renderDistance = 16;
            this.simulationDistance = 10;
            this.maxPlayers = 20;
            this.rconPort = 25575;
        }
    }
}