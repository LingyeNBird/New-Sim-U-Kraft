package com.xiaoliang.simukraft.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class PinManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SET_TYPE = new TypeToken<Set<String>>(){}.getType();
    
    private final File pinFile;
    private Set<String> pinnedBuildings;
    
    public PinManager() {
        // 保存到游戏目录下的 config/simukraft_pinned.json
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.pinFile = new File(configDir, "simukraft_pinned.json");
        this.pinnedBuildings = load();
    }
    
    private Set<String> load() {
        if (!pinFile.exists()) {
            return new HashSet<>();
        }
        try (FileReader reader = new FileReader(pinFile)) {
            Set<String> loaded = GSON.fromJson(reader, SET_TYPE);
            return loaded != null ? loaded : new HashSet<>();
        } catch (IOException e) {
            System.err.println("[PinManager] Failed to load pinned buildings: " + e.getMessage());
            return new HashSet<>();
        }
    }
    
    public void save(Set<String> pinnedBuildings) {
        this.pinnedBuildings = new HashSet<>(pinnedBuildings);
        try (FileWriter writer = new FileWriter(pinFile)) {
            GSON.toJson(this.pinnedBuildings, writer);
        } catch (IOException e) {
            System.err.println("[PinManager] Failed to save pinned buildings: " + e.getMessage());
        }
    }
    
    public Set<String> getPinnedBuildings() {
        return new HashSet<>(pinnedBuildings);
    }
    
    public boolean isPinned(String fileName) {
        return pinnedBuildings.contains(fileName);
    }
    
    public void pin(String fileName) {
        pinnedBuildings.add(fileName);
        save(pinnedBuildings);
    }
    
    public void unpin(String fileName) {
        pinnedBuildings.remove(fileName);
        save(pinnedBuildings);
    }
}
