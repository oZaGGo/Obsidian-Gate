package com.obsidiangate.mcpanel.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BackupDTO {
    private String path;
    private String world;
    private String alias;
    private long size; // bytes
    private LocalDateTime backupDate;
}
