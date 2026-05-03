package com.xiaoliang.simukraft.world;

import com.xiaoliang.simukraft.network.NetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.UUID;
public class PopulationData extends SavedData {
    private static final String DATA_NAME = "simukraft_population";
    private int population = 0;

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population, ServerLevel level) {
        this.population = population;
        setDirty();
        // 同步HUD数据给所有玩家
        syncToAllPlayers(level);
    }
    
    // 重载方法，兼容旧代码
    public void setPopulation(int population) {
        this.population = population;
        setDirty();
    }

    public void addPopulation(ServerLevel level) {
        this.population++;
        setDirty();
        // 同步HUD数据给所有玩家
        syncToAllPlayers(level);
    }

    // 重载方法，兼容旧代码
    public void addPopulation() {
        this.population++;
        setDirty();
    }

    public void removePopulation(ServerLevel level) {
        if (this.population > 0) {
            this.population--;
            setDirty();
            // 同步HUD数据给所有玩家
            syncToAllPlayers(level);
        }
    }

    // 重载方法，兼容旧代码
    public void removePopulation() {
        if (this.population > 0) {
            this.population--;
            setDirty();
        }
    }

    public void syncToAllPlayers(ServerLevel level) {
        // 发送新的HUD数据同步包给所有玩家
        int worldPopulation = this.population;
        
        // 获取当前天数
        SimukraftWorldData worldData = SimukraftWorldData.get(level);
        int currentDay = worldData.getCurrentDay();
        
        // 针对每个玩家发送个性化的HUD数据
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            // 针对每个玩家获取城市数据
            String cityName = "";
            double cityFunds = 0.0;
            int cityPopulation = 0;
            
            // 从CityData获取玩家城市信息
            CityData cityData = CityData.get(level);
            String playerName = player.getName().getString();
            UUID cityId = cityData.getPlayerCityId(playerName);
            if (cityId != null) {
                CityData.CityInfo cityInfo = cityData.getCity(cityId);
                if (cityInfo != null) {
                    cityName = cityInfo.getCityName();
                    cityFunds = cityInfo.getFunds();
                    cityPopulation = cityInfo.getCitizenIds().size();
                }
            }
            
            // 发送HUD数据同步包
            NetworkManager.sendHUDDataToPlayer(currentDay, worldPopulation, cityName, cityFunds, cityPopulation, player);
        }
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        tag.putInt("population", population);
        return tag;
    }

    public static PopulationData load(CompoundTag tag) {
        PopulationData data = new PopulationData();
        data.population = tag.getInt("population");
        return data;
    }

    public static PopulationData get(ServerLevel level) {
        // 确保总是使用主世界的数据存储，而不是当前维度的数据存储
        ServerLevel overworld = level.getServer().getLevel(java.util.Objects.requireNonNull(Level.OVERWORLD));
        if (overworld == null) {
            overworld = level;
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
                PopulationData::load,
                PopulationData::new,
                DATA_NAME
        );
    }
}
