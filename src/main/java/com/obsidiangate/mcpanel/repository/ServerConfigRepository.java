package com.obsidiangate.mcpanel.repository;

import com.obsidiangate.mcpanel.model.ServerConfigModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerConfigRepository extends JpaRepository<ServerConfigModel, Long> {
    default ServerConfigModel findActiveConfig() {
        return findAll().stream().findFirst().orElse(new ServerConfigModel());
    }
}