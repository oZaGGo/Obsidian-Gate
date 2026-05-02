package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.util.system.ZipCompressor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;


@Service
public class WorldManagementService {

    @Autowired
    private ServerRuntimeService serverRuntimeService;

    @Autowired
    private ZipCompressor zipCompressor;

    private final String serverPath = Paths.get(System.getProperty("user.dir"), "mc_server").toString();
    private final String backupsPath = Paths.get(System.getProperty("user.dir"), "mc_backups").toString();

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

    public void createBackup(String name, String alias) {
        new Thread( () -> {
            if(serverRuntimeService.isRunning()){
                serverRuntimeService.sendCommand("save-off");
                String response = serverRuntimeService.sendCommandWithResponse("save-all", "Saved the game", 500);

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

                Path worldPath = Paths.get(serverPath, name);
                Path pathFinal = Paths.get(backupsPath, alias + "-backup-" + timestamp + ".zip");

                if(!response.isEmpty()) {
                    zipCompressor.getZip(worldPath, pathFinal, serverRuntimeService);
                }
            }else{
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

                Path worldPath = Paths.get(serverPath,name);
                Path pathFinal = Paths.get(backupsPath, alias + "-backup-" + timestamp + ".zip");

                zipCompressor.getZip(worldPath, pathFinal,null);

            }


        }).start();
    }
}