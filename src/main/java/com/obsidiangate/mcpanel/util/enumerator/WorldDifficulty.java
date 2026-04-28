package com.obsidiangate.mcpanel.util.enumerator;

import lombok.Getter;

@Getter
public enum WorldDifficulty {
    PEACEFUL("peaceful"),
    EASY("easy"),
    NORMAL("normal"),
    HARD("hard");

    private final String key;

    WorldDifficulty(String key) {
        this.key = key;
    }

    public static WorldDifficulty fromKey(String key) {
        for (WorldDifficulty diff : values()) {
            if (diff.key.equalsIgnoreCase(key)) return diff;
        }
        return NORMAL;
    }
}