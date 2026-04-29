package com.obsidiangate.mcpanel.util.enumerator;

public enum ChatCommandType {
    ECHO("echo"),
    INFO("info"),
    ASK("ask");

    private final String value;

    ChatCommandType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}