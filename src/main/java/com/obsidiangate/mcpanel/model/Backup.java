package com.obsidiangate.mcpanel.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "SERV_BACKUP")
@Data
public class Backup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String world;

    @Column(unique = true, nullable = false, length = 50)
    private String alias;

    @Column(nullable = false)
    private long size; // bytes

    @Column(name = "backup_date")
    private LocalDateTime backupDate;

}
