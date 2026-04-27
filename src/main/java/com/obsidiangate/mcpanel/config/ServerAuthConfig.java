package com.obsidiangate.mcpanel.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerAuthConfig {
    private long authTimeMins = 3600; // Mins
}
