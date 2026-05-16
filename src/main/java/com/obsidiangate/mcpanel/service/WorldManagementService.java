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

    public void deleteWorldFolder(String worldName) {
        Path worldPath = Paths.get(System.getProperty("user.dir"), "mc_server", worldName);

        // Delete associated backups (physical files and DB records)
        List<Backup> backups = backupRepository.findByWorld(worldName);
        for (Backup backup : backups) {
            try {
                Path backupPath = Paths.get(backup.getPath());
                Files.deleteIfExists(backupPath);
            } catch (IOException e) {
                System.err.println("Could not delete backup file: " + backup.getPath());
            }
        }
        backupRepository.deleteAll(backups);

        // Delete the world folder
        if (Files.exists(worldPath)) {
            try {
                Files.walk(worldPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);

                System.out.println("Successfully deleted world folder and backups for: " + worldName);
            } catch (IOException e) {
                throw new RuntimeException("Could not delete world folder. Files might be in use: " + e.getMessage());
            }
        }
    }

    public void createBackup(String name, String alias) {
        new Thread(() -> {
            isProcessingBackup = true;

            try {
                Path worldPath = Paths.get(serverPath, name);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
                String zipName = alias.isEmpty() ? timestamp + "-BACKUP"  : alias;
                Path pathFinal = Paths.get(backupsPath, zipName + "_" + timestamp + ".zip");

                if (serverRuntimeService.isRunning()) {
                    serverRuntimeService.sendCommand("save-off");
                    // Attempt to save the world before compression
                    String response = serverRuntimeService.sendCommandWithResponse("save-all flush", "Saved the game", 500);

                    if (!response.isEmpty()) {
                        boolean success = zipCompressor.getZip(worldPath, pathFinal, serverRuntimeService, this, name, zipName);
                        if (success) {
                            registerBackupInDatabase(pathFinal, zipName, name);
                            serverRuntimeService.sendCommand("say [SYSTEM] A backup of the world was created. You can find it in the server panel.");
                        } else {
                            cleanupFailedBackup();
                        }
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
        isProcessingBackup = false;
        if (serverRuntimeService.isRunning()) {
            serverRuntimeService.sendCommand("save-on");
        }
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