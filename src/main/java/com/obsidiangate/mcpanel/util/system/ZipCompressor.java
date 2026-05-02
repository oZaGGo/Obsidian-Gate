package com.obsidiangate.mcpanel.util.system;

import com.obsidiangate.mcpanel.service.ServerRuntimeService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ZipCompressor {

    public void getZip(Path worldPath, Path outputPath, ServerRuntimeService serverRuntimeService) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath.toFile()))) {
            Files.walk(worldPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(worldPath.relativize(path).toString());

                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                            // if not null server is running
                            if (serverRuntimeService != null) {
                                serverRuntimeService.sendCommand("save-on");
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Can't copy the carpets in .zip \"" + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.out.println();
            throw new RuntimeException("Can't create the .zip" + e);
        }
    }
}
