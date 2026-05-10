package com.obsidiangate.mcpanel.dto;

import lombok.Data;

@Data
public class ServerConfigDTO {
    private String name;
    private String description;
    private Integer maxGbRam;
    private Integer renderDistance;
    private Integer simulationDistance;
    private Integer maxPlayers;
    private Integer serverPort;
}