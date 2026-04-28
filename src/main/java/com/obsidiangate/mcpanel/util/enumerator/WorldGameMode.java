package com.obsidiangate.mcpanel.util.enumerator;

import lombok.Getter;

@Getter
public enum WorldGameMode {
    SURVIVAL("survival"),
    CREATIVE("creative"),
    ADVENTURE("adventure"),
    SPECTATOR("spectator");

    private final String key;

    WorldGameMode(String key) {
        this.key = key;
    }

    public static WorldGameMode fromKey(String key) {
        for (WorldGameMode mode : values()) {
            if (mode.key.equalsIgnoreCase(key)) return mode;
        }
        return SURVIVAL;
    }
}