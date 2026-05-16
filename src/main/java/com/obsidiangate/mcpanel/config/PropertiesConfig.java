package com.obsidiangate.mcpanel.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Data
public class PropertiesConfig {

    //Server config
    private String levelName;
    private String levelSeed;
    private String difficulty;
    private String gamemode;
    private String hardcore;

    //World config
    private String motd;
    private String maxPlayers;
    private String viewDistance;
    private String simulationDistance;
    private String serverPort = "25565";
    private String spawnProtection = "16";
}