package com.obsidiangate.mcpanel.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenerationConfig {
    private int maxOutputTokens;
    private double temperature;
}