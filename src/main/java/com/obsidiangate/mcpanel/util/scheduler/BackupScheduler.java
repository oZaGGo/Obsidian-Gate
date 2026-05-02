package com.obsidiangate.mcpanel.util.scheduler;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.model.World;
import com.obsidiangate.mcpanel.repository.WorldRepository;
import com.obsidiangate.mcpanel.service.LogService;
import com.obsidiangate.mcpanel.service.WorldManagementService;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BackupScheduler {

    @Autowired
    private WorldManagementService worldManagementService;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private LogService logService;

    @Autowired
    private AppConfig appConfig;

    // Every 12h (00:00, 12:00)
    @Scheduled(cron = "0 0 */12 * * *")
    public void scheduleTwelveHours() {
        if (appConfig.getBackupTime().equals("12h")) {
            runAutoBackups("Auto-12h");
        }
    }

    // Daily at 06:00 AM
    @Scheduled(cron = "0 0 6 * * *")
    public void scheduleDaily() {
        if (appConfig.getBackupTime().equals("daily")) {
            runAutoBackups("Auto-daily");
        }
    }

    // Weekly - Every Monday at 06:00 AM
    @Scheduled(cron = "0 0 6 * * MON")
    public void scheduleWeekly() {
        if (appConfig.getBackupTime().equals("weeekly")) {
            runAutoBackups("Auto-weekly");
        }
    }

    // Debug task - Every minute
    @Scheduled(cron = "0 * * * * *")
    public void scheduleDebug() {
        //System.out.println("[DEBUG-SCHEDULER] Heartbeat at: " + LocalDateTime.now());
    }

    private void runAutoBackups(String prefix) {
        Optional<World> worldOpt = worldRepository.findCurrentWorld();

        if (worldOpt.isEmpty()) {
            System.out.println("[SCHEDULER] No world found for backup.");
            return;
        }

        World world = worldOpt.get();

        try {
            // Check if another backup is already running to avoid overlaps
            if (worldManagementService.isBackupFinished()) {
                worldManagementService.createBackup(world.getName(), prefix + "-" + world.getName());
                logService.registerEntry(appConfig.getSystemUsrToken(),
                        "Scheduled backup started for world: " + world.getName(), LogEntryType.WORLDMANAGEMENT);
            } else {
                System.out.println("[SCHEDULER] Backup skipped for " + world.getName() + ": Another process is active.");
            }
        } catch (Exception e) {
            System.err.println("Scheduled backup failed for " + world.getName() + ": " + e.getMessage());
        }
    }
}