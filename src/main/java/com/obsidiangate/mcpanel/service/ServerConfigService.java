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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class ServerConfigService {

    @Autowired
    private ServerConfigRepository repository;

    @Autowired
    private ServerConfig serverConfigInApp;

    @Autowired
    private PropertiesService propertiesService;

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
        current.setServerPort(dto.getRconPort());

        repository.save(current);
        serverConfigInApp.refresh();
        propertiesService.updateProperties();
        propertiesService.saveToFile();
    }

    public Resource getIconResource() {
        File file = new File(ICON_PATH);
        if (!file.exists()) return null;
        return new FileSystemResource(file);
    }

    public void saveIcon(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("The file is empty.");
        }

        BufferedImage image = ImageIO.read(file.getInputStream());

        if (image == null) {
            throw new IOException("Invalid image format.");
        }

        if (image.getWidth() != 64 || image.getHeight() != 64) {
            throw new IOException("Icon must be exactly 64x64 pixels. Current: "
                    + image.getWidth() + "x" + image.getHeight());
        }

        File dest = new File(ICON_PATH);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        boolean result = ImageIO.write(image, "png", dest);

        if (!result) {
            throw new IOException("Could not save the PNG file. Check folder permissions.");
        }

        System.out.println("Server icon updated and rewritten successfully at: " + ICON_PATH);
    }
}