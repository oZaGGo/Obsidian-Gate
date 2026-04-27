package com.obsidiangate.mcpanel.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Data
@Getter
@Setter
public class ServerConfig {
    private int maxGbRam = 16; // Max GB of RAM that can be allocated to the server
    private String name = "ObsidianServer"; // Name of the server
    private String description = "ObsidianServer"; // Description of the server
    private int renderDistance = 16; // Minecraft chunk loading distance
    private int simulationDistance = 16; // Minecrfat chunk simulation distance
}
