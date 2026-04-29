package com.obsidiangate.mcpanel.dto;

import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogEntryDTO {
    LogEntryType type;
    String authorName;
    String trace;
    LocalDateTime timestamp;
}

