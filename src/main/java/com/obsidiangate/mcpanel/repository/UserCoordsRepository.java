package com.obsidiangate.mcpanel.repository;

import com.obsidiangate.mcpanel.model.UserCoords;
import com.obsidiangate.mcpanel.model.World;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCoordsRepository extends JpaRepository<UserCoords, Long> {
    List<UserCoords> findByPlayerNameAndWorldAndDimension(String playerName, World world, String dimension);
    @Transactional
    void deleteByPlayerNameAndWorldAndAlias(String playerName, World world, String alias);
}