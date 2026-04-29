package com.obsidiangate.mcpanel.repository;

import com.obsidiangate.mcpanel.model.LogEntry;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    // When this receives LocalDateTime.now().minusHours(24) this return last 24h log
    List<LogEntry> findByTimestampAfterOrderByTimestampDesc(LocalDateTime timestamp);

    // Same but with type filter
    List<LogEntry> findByTypeAndTimestampAfterOrderByTimestampDesc(LogEntryType type, LocalDateTime timestamp);
}