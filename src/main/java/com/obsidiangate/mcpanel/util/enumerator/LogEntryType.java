package com.obsidiangate.mcpanel.util.enumerator;

public enum LogEntryType {
    AUTHORIZATION,      // Login, register, manager etc.
    SERVERCONFIG,       // Server config changes
    WORLDCONFIG,        // World config changes
    WORLDMANAGEMENT,     // World management
    SERVERRUNTIME // Server actions (start, stop, restart)
}