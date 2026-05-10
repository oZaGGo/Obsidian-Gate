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

    @Builder.Default
    @Column(nullable = false)
    private Integer maxGbRam = 2;

    @Builder.Default
    @Column(nullable = false, length = 100)
    private String name = "Minecraft Server";

    @Builder.Default
    @Column(nullable = false, length = 500)
    private String description = "Server managed by ObsidianGate";

    @Builder.Default
    @Column(nullable = false)
    private Integer renderDistance = 16;

    @Builder.Default
    @Column(nullable = false)
    private Integer simulationDistance = 10;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxPlayers = 20;

    @Builder.Default
    @Column(nullable = false)
    private Integer serverPort = 25565;

    @Builder.Default
    @Column(nullable = false)
    private boolean firstSetup = true;
}