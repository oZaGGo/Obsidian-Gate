package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.ServerConfig;
import com.obsidiangate.mcpanel.dto.ServerConfigDTO;
import com.obsidiangate.mcpanel.model.ServerConfigModel;
import com.obsidiangate.mcpanel.repository.ServerConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class ServerConfigService {

    @Autowired
    private ServerConfigRepository repository;

    @Autowired
    private ServerConfig serverConfigInApp;

    private final String ICON_PATH = System.getProperty("user.dir") + "/mc_server/server-icon.png";

    public ServerConfigModel getConfig() {
        return repository.findActiveConfig();
    }

    public void updateConfig(ServerConfigDTO dto) {
        ServerConfigModel current = repository.findActiveConfig();

        if (current == null) {
            current = new ServerConfigModel();
        }

        current.setName(dto.getName());
        current.setDescription(dto.getDescription());
        current.setMaxGbRam(dto.getMaxGbRam());
        current.setRenderDistance(dto.getRenderDistance());
        current.setSimulationDistance(dto.getSimulationDistance());
        current.setMaxPlayers(dto.getMaxPlayers());
        current.setRconPort(dto.getRconPort());

        repository.save(current);
        serverConfigInApp.refresh();
    }

    public Resource getIconResource() {
        File file = new File(ICON_PATH);
        if (!file.exists()) return null;
        return new FileSystemResource(file);
    }

    public void saveIcon(MultipartFile file) throws IOException {
        File dest = new File(ICON_PATH);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        file.transferTo(dest);
    }
}