package com.obsidiangate.mcpanel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SERV_WORLD")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private String seed;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private String gamemode;

    @Column(nullable = false)
    private boolean hardcore;

    @Column(name = "is_current", nullable = false)
    private boolean current;
}