package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.dto.BackupDTO;
import com.obsidiangate.mcpanel.model.Backup;
import com.obsidiangate.mcpanel.repository.BackupRepository;
import com.obsidiangate.mcpanel.util.system.ZipCompressor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class WorldManagementService {

    @Autowired
    private ServerRuntimeService serverRuntimeService;

    @Autowired
    private ZipCompressor zipCompressor;

    @Autowired
    private BackupRepository backupRepository;

    private final String serverPath = Paths.get(System.getProperty("user.dir"), "mc_server").toString();
    private final String backupsPath = Paths.get(System.getProperty("user.dir"), "mc_backups").toString();

    private boolean isProcessingBackup = false;

    private boolean backupThreadFailed = false;

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
        new Thread(() -> {
            String zipName = alias.isEmpty() ? "backup" : alias;
            isProcessingBackup = true;
            backupThreadFailed = false;

            try {
                Path worldPath = Paths.get(serverPath, name);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
                Path pathFinal = Paths.get(backupsPath, zipName + "_" + timestamp + ".zip");

                if (serverRuntimeService.isRunning()) {
                    serverRuntimeService.sendCommand("save-off");
                    // Attempt to save the world before compression
                    String response = serverRuntimeService.sendCommandWithResponse("save-all", "Saved the game", 500);

                    if (!response.isEmpty()) {
                        boolean success = zipCompressor.getZip(worldPath, pathFinal, serverRuntimeService, this, name, zipName);
                        if (success) registerBackupInDatabase(pathFinal, zipName, name);
                    } else {
                        // Abort if save-all fails
                        cleanupFailedBackup();
                    }
                    isProcessingBackup = false;
                } else {
                    // Server is offline, direct compression
                    boolean success = zipCompressor.getZip(worldPath, pathFinal, null, this, name, zipName);
                    if (success){
                        registerBackupInDatabase(pathFinal, zipName, name);
                    }else {
                        cleanupFailedBackup();
                    }
                    isProcessingBackup = false;
                }
            } catch (Exception e) {
                // Catching exceptions inside the thread to prevent silent hangs
                System.err.println("Backup thread error: " + e.getMessage());
                cleanupFailedBackup();
            }
        }).start();
    }

    private void cleanupFailedBackup() {
        backupThreadFailed = true;
        isProcessingBackup = false;
        if (serverRuntimeService.isRunning()) {
            serverRuntimeService.sendCommand("save-on");
        }
    }

    public boolean didBackupThreadFail() {
        return backupThreadFailed;
    }

    public void registerBackupInDatabase(Path path, String alias, String worldName) {
        try {
            File file = path.toFile();

            Backup backup = new Backup();
            backup.setAlias(alias);
            backup.setPath(path.toString());
            backup.setWorld(worldName);
            backup.setSize(file.length());
            backup.setBackupDate(LocalDateTime.now());

            backupRepository.save(backup);
        } catch (Exception e) {
            System.err.println("Error saving backup record to DB: " + e.getMessage());
        }
    }

    public void restoreBackup(String alias, String worldName) {
        new Thread(() -> {
            isProcessingBackup = true;

            try {
                if (serverRuntimeService.isRunning()) {
                    isProcessingBackup = false;
                    throw new RuntimeException("Cannot restore backup while the server is running. Please stop the server first.");
                }

                Backup backup = backupRepository.findByAlias(alias)
                        .orElseThrow(() -> new RuntimeException("Backup not found with alias: " + alias));

                Path zipPath = Paths.get(backup.getPath());
                Path worldPath = Paths.get(serverPath, worldName);

                zipCompressor.extractZip(zipPath, worldPath, this);

            } catch (Exception e) {
                isProcessingBackup = false;
                System.err.println("Error restoring backup: " + e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        }).start();
    }

    public void deleteBackup(String alias) {
        Backup backup = backupRepository.findByAlias(alias)
                .orElseThrow(() -> new RuntimeException("Backup not found with alias: " + alias));

        try {
            Path path = Paths.get(backup.getPath());

            if (Files.exists(path)) {
                Files.delete(path);
            }
            backupRepository.delete(backup);

        } catch (IOException e) {
            throw new RuntimeException("Could not delete backup file: " + e.getMessage());
        }
    }

    public boolean isBackupFinished() {
        return !isProcessingBackup;
    }

    public void backupFinished() {
        isProcessingBackup = false;
    }

    public List<BackupDTO> getAllBackupsSorted() {
        return backupRepository.findAllByOrderByBackupDateDesc()
                .stream()
                .map(b -> {
                    BackupDTO dto = new BackupDTO();
                    dto.setAlias(b.getAlias());
                    dto.setSize(b.getSize());
                    dto.setBackupDate(b.getBackupDate());
                    dto.setPath(Paths.get(b.getPath()).getFileName().toString());
                    dto.setWorld(b.getWorld());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}