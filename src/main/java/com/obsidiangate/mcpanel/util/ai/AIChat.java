package com.obsidiangate.mcpanel.util.ai;

import com.obsidiangate.mcpanel.config.AIConfig;
import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.dto.ai.Content;
import com.obsidiangate.mcpanel.dto.ai.GeminiRequest;
import com.obsidiangate.mcpanel.dto.ai.GenerationConfig;
import com.obsidiangate.mcpanel.dto.ai.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AIChat {

    private final AppConfig appConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=";

    private static final String SYSTEM_CONTEXT = new AIConfig().getContext();
    private static final int TOKEN_LIMIT = new AIConfig().getTokenLimit();
    private static final double TEMPERATURE = new AIConfig().getTemperature();


    public String askAI(String userPrompt) {
        try {
            String apiKey = appConfig.getAiToken();
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-token-here")) {
                return "Error: AI Token not configured in server-config.json";
            }

            List<Content> contents = List.of(
                    new Content("user", List.of(new Part(SYSTEM_CONTEXT))),
                    new Content("model", List.of(new Part("Understood. I am ObsidianAI, ready to assist with Minecraft questions."))),
                    new Content("user", List.of(new Part(userPrompt)))
            );

            GenerationConfig genConfig = new GenerationConfig(TOKEN_LIMIT, TEMPERATURE);

            GeminiRequest request = new GeminiRequest(contents,genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            return "AI Error: " + e.getMessage();
        }
    }
}