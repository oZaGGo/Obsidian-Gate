package com.obsidiangate.mcpanel.util.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ApplicationReadyListener {

    private final Environment environment;

    public ApplicationReadyListener(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("local.server.port");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String host;

        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "localhost";
        }

        System.out.println("\n----------------------------------------------------------");
        System.out.println("   ¡ObsidianGate started succesful!");
        System.out.println("   Local:    http://localhost:" + port + contextPath);
        System.out.println("   Red:      http://" + host + ":" + port + contextPath);
        System.out.println("----------------------------------------------------------\n");
    }
}