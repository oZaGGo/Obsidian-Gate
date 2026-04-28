package com.obsidiangate.mcpanel.dto;

import lombok.Data;

@Data
public class WorldDTO {
    private String nombre;
    private String seed;
    private String difficulty;
    private String gamemode;
    private boolean hardcore;
    private boolean current;
}