package com.obsidiangate.mcpanel.util.system;

import com.obsidiangate.mcpanel.service.ServerRuntimeService;
import com.obsidiangate.mcpanel.service.WorldManagementService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
public class ZipCompressor {

    public boolean getZip(Path worldPath, Path outputPath, ServerRuntimeService serverRuntimeService, WorldManagementService worldManagementService, String worldName, String zipName) {
        if (!Files.exists(worldPath)) {
            if (serverRuntimeService != null) {
                serverRuntimeService.sendCommand("save-on");
            }
            worldManagementService.backupFinished();
            return false;
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath.toFile()))) {
            Files.walk(worldPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(worldPath.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Can't copy files: " + e.getMessage());
                        }
                    });

            if (serverRuntimeService != null) {
                serverRuntimeService.sendCommand("save-on");
            }
            worldManagementService.backupFinished();
            worldManagementService.registerBackupInDatabase(outputPath, zipName, worldName);
            return true;
        } catch (IOException e) {
            if (serverRuntimeService != null) {
                serverRuntimeService.sendCommand("save-on");
            }
            worldManagementService.backupFinished();
            return false;
        }
    }

    public void extractZip(Path zipPath, Path worldPath, WorldManagementService worldManagementService) {
        try {
            if (!Files.exists(zipPath)) {
                throw new RuntimeException("Backup file not found: " + zipPath);
            }

            if (Files.exists(worldPath)) {
                Files.walk(worldPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            Files.createDirectories(worldPath);

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path newPath = worldPath.resolve(entry.getName());

                    if (entry.isDirectory()) {
                        Files.createDirectories(newPath);
                    } else {
                        Files.createDirectories(newPath.getParent());
                        Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            worldManagementService.backupFinished();

        } catch (IOException e) {
            worldManagementService.backupFinished();
            throw new RuntimeException("Failed to restore backup: " + e.getMessage());
        }
    }
}
