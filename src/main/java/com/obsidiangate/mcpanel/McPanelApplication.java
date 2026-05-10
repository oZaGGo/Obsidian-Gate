package com.obsidiangate.mcpanel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class McPanelApplication {

	public static void main(String[] args) {
		SpringApplication.run(McPanelApplication.class, args);
	}

}