package com.obsidiangate.mcpanel.repository;

import com.obsidiangate.mcpanel.model.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {

    Optional<Backup> findByAlias(String alias);

    List<Backup> findAllByOrderByBackupDateDesc();

    boolean existsByAlias(String alias);

    void deleteByAlias(String alias);
}