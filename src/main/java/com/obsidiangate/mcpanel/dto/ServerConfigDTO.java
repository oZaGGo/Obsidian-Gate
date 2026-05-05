package com.obsidiangate.mcpanel.dto;

import lombok.Data;

@Data
public class ServerConfigDTO {
    private String name;
    private String description;
    private int maxGbRam;
    private int renderDistance;
    private int simulationDistance;
    private int maxPlayers;
    private int serverPort;
}