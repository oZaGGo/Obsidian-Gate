package com.obsidiangate.mcpanel.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Data
@Getter
@Setter
public class ServerAuthConfig {
    private long authTimeMins = 20; // Mins
    private boolean firstSetup = true;
}
