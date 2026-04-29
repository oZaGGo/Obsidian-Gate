package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.dto.LogEntryDTO;
import com.obsidiangate.mcpanel.model.LogEntry;
import com.obsidiangate.mcpanel.model.UserAuth;
import com.obsidiangate.mcpanel.repository.LogEntryRepository;
import com.obsidiangate.mcpanel.repository.UserAuthRepository;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LogService {

    @Autowired
    private LogEntryRepository logRepository;

    @Autowired
    private UserAuthRepository userAuthRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_FILE_PATH = Paths.get("server.log");

    @Transactional
    public void registerEntry(String token, String trace, LogEntryType type) {
        try {
            Optional<UserAuth> userOpt = userAuthRepository.findByToken(token);

            UserAuth author = userOpt.orElse(null);
            String authorName = (author != null) ? author.getUsername() : "SYSTEM";

            LogEntry entry = new LogEntry();
            entry.setType(type);
            entry.setTrace(trace);
            entry.setAuthor(author);
            logRepository.save(entry);

            writeToLogFile(authorName, trace, type);

        } catch (Exception e) {
            System.err.println("Error registering log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writeToLogFile(String author, String trace, LogEntryType type) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String logLine = String.format("[%s] [%s] [%s]: %s%n",
                    timestamp, type.name(), author, trace);

            Files.writeString(LOG_FILE_PATH, logLine,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

        } catch (IOException e) {
            System.err.println("Critical Error writing to server.log: " + e.getMessage());
        }
    }

    public List<LogEntryDTO> getLastLogs() {
        LocalDateTime limit = LocalDateTime.now().minusHours(24);
        return logRepository.findByTimestampAfterOrderByTimestampDesc(limit)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private LogEntryDTO convertToDTO(LogEntry entry) {
        String authorName = (entry.getAuthor() != null)
                ? entry.getAuthor().getUsername()
                : "SYSTEM";

        LogEntryDTO dto = new LogEntryDTO();
        dto.setType(entry.getType());
        dto.setAuthorName(authorName);
        dto.setTrace(entry.getTrace());
        dto.setTimestamp(entry.getTimestamp());
        return dto;
    }
}