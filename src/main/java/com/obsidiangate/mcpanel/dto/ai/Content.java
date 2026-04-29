package com.obsidiangate.mcpanel.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@AllArgsConstructor
@Getter
@Setter
public class Content {
    private String role;
    private List<Part> parts;
}