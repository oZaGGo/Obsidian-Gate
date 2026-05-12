package com.obsidiangate.mcpanel.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String username;
    private String password;
    private String token;
    private boolean isAdmin;
    private boolean setupCompleted;
    private LocalDateTime lastConn;
}