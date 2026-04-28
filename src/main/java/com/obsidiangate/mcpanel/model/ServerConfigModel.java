package com.obsidiangate.mcpanel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SERV_CONF")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfigModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int maxGbRam = 2;

    @Column(nullable = false, length = 100)
    private String name = "Minecraft Server";

    @Column(nullable = false, length = 500)
    private String description = "Server managed by ObsidianGate";

    @Column(nullable = false)
    private int renderDistance = 16;

    @Column(nullable = false)
    private int simulationDistance = 10;

    @Column(nullable = false)
    private int maxPlayers = 20;

    @Column(nullable = false)
    private int rconPort = 25575;

    @Column(nullable = false)
    private boolean firstSetup = true;
}