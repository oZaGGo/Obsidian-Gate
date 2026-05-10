package com.obsidiangate.mcpanel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "USER_COORDS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoords {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private String alias;

    @Column(nullable = false)
    private String coordinates;

    @Column(nullable = false)
    private String dimension;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "world_id", nullable = false)
    private World world;
}