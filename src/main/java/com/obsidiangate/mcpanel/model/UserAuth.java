package com.obsidiangate.mcpanel.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "USR_AUTH")
@Data
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String token;

    @Column(name = "last_conn")
    private LocalDateTime lastConn;

    @Column(nullable = false)
    private boolean isAdmin;

    @Column(nullable = false)
    private boolean setupCompleted;

}