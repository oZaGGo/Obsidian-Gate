package com.obsidiangate.mcpanel.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Data
@Getter
@Setter
public class AppConfig {
    private long authTimeMins = 20; // Mins
    private boolean firstSetup = true;
    private String aiToken = "api-token-here";
    private String systemUsrToken = "system-user-token-here";
    private String backupTime = "12h";
}
