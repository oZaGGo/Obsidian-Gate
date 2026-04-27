package com.obsidiangate.mcpanel.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class SystemMetricsDTO {
    private double cpuUsage;
    private double ramUsed;
    private double ramTotal;
}
