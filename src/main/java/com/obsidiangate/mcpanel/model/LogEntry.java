package com.obsidiangate.mcpanel.model;

import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "SERV_LOG")
@Data
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LogEntryType type;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserAuth author;

    @Column(columnDefinition = "TEXT")
    private String trace;

    private LocalDateTime timestamp;

    public LogEntry() {
        this.timestamp = LocalDateTime.now();
    }
}