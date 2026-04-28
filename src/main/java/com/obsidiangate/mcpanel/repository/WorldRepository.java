package com.obsidiangate.mcpanel.repository;

import com.obsidiangate.mcpanel.model.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorldRepository extends JpaRepository<World, Long> {

    @Query("SELECT w FROM World w WHERE w.current = true")
    Optional<World> findCurrentWorld();

    Optional<World> findByName(String name);
}