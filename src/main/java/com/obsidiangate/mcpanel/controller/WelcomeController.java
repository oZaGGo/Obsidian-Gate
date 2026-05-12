package com.obsidiangate.mcpanel.controller;

import com.obsidiangate.mcpanel.service.WelcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/welcome")
public class WelcomeController {

    @Autowired
    private WelcomeService welcomeService;

    @GetMapping("/versions")
    public List<String> getVersions() {
        System.out.println(welcomeService.getAvailableVersions());
        return welcomeService.getAvailableVersions();
    }
}