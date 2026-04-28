package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.dto.WorldDTO;
import com.obsidiangate.mcpanel.model.World;
import com.obsidiangate.mcpanel.repository.WorldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorldConfigService {

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private WorldManagementService worldManagementService;

    @Autowired
    private PropertiesService propertiesService;

    public List<WorldDTO> getAllWorlds() {
        return worldRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public WorldDTO getCurrentWorld() {
        return worldRepository.findCurrentWorld()
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional
    public void saveWorld(WorldDTO dto) {
        World world = worldRepository.findByName(dto.getNombre())
                .orElse(new World());

        world.setName(dto.getNombre());
        world.setSeed(dto.getSeed());
        world.setDifficulty(dto.getDifficulty());
        world.setGamemode(dto.getGamemode());
        world.setHardcore(dto.isHardcore());
        world.setCurrent(true);

        worldRepository.save(world);

        setActiveWorld(dto.getNombre());

        propertiesService.updateProperties();
        propertiesService.saveToFile();
    }

    @Transactional
    public void setActiveWorld(String worldName) {
        List<World> worlds = worldRepository.findAll();
        World target = null;

        for (World w : worlds) {
            if (w.getName().equals(worldName)) {
                w.setCurrent(true);
                target = w;
            } else {
                w.setCurrent(false);
            }
        }

        if (target != null) {
            worldRepository.saveAll(worlds);
            propertiesService.updateProperties();
            propertiesService.saveToFile();
        }
    }

    @Transactional
    public void deleteWorld(String worldName) {
        World world = worldRepository.findByName(worldName)
                .orElseThrow(() -> new RuntimeException("World not found"));

        if (world.isCurrent()) {
            throw new RuntimeException("Cannot delete the active world");
        }

        worldRepository.delete(world);
        worldManagementService.deleteWorldFolder(worldName);
    }

    private WorldDTO convertToDTO(World world) {
        WorldDTO dto = new WorldDTO();
        dto.setNombre(world.getName());
        dto.setSeed(world.getSeed());
        dto.setDifficulty(world.getDifficulty());
        dto.setGamemode(world.getGamemode());
        dto.setHardcore(world.isHardcore());
        dto.setCurrent(world.isCurrent());
        return dto;
    }
}