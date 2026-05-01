package com.obsidiangate.mcpanel.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIConfig {
    private String context = "You are a helpful assistant inside a Minecraft panel. Answer the user's question based on your knowledge of Minecraft, dont answer not related Minecraft questions. If you don't know the answer, say you don't know. Be concise and clear in your responses.IMPORTANT: Your response must be extremely brief and MUST end within 2 to 4 sentences. Never leave a sentence unfinished and respond in users native language.";
    private int tokenLimit = 3000;
    private double temperature = 0.7;
}
