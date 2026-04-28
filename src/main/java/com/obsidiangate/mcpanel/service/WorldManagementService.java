package com.obsidiangate.mcpanel.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@Service
public class WorldManagementService {

    public void deleteWorldFolder(String worldName) {
        Path worldPath = Paths.get(System.getProperty("user.dir"), "mc_server", worldName);

        if (Files.exists(worldPath)) {
            try {
                // Files.walk generates an stream with all files and subdirectories
                Files.walk(worldPath)
                        // Order by reverse to delete files before directories
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);

                System.out.println("Successfully deleted world folder: " + worldName);
            } catch (IOException e) {
                // If an error occurs, it might be because the server is still using files in that world folder. In that case, we throw a RuntimeException to be handled by the controller.
                throw new RuntimeException("Could not delete world folder. Files might be in use by the Minecraft server: " + e.getMessage());
            }
        }
    }
}