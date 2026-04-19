package com.xiaoliang.simukraft.event;

import net.minecraftforge.eventbus.api.Event;

import java.util.List;
import java.util.UUID;

/**
 * 城市数据发生变更时触发的 Forge 事件。
 * <p>
 * 在 CityData.setDirty() 时发布到 MinecraftForge.EVENT_BUS，
 * 携带所有城市的快照数据，供外部模组（如 CityChat）监听并同步缓存。
 */
public class CityDataChangedEvent extends Event {

    private final List<CitySnapshot> cities;

    public CityDataChangedEvent(List<CitySnapshot> cities) {
        this.cities = cities;
    }

    public List<CitySnapshot> getCities() {
        return cities;
    }

    /**
     * 城市快照——只读、轻量，不泄露 CityData 内部结构。
     */
    public static class CitySnapshot {
        private final UUID cityId;
        private final String cityName;
        private final UUID mayorId;
        private final List<String> officialNames;

        public CitySnapshot(UUID cityId, String cityName, UUID mayorId, List<String> officialNames) {
            this.cityId = cityId;
            this.cityName = cityName;
            this.mayorId = mayorId;
            this.officialNames = officialNames;
        }

        public UUID getCityId() { return cityId; }
        public String getCityName() { return cityName; }
        public UUID getMayorId() { return mayorId; }
        public List<String> getOfficialNames() { return officialNames; }
    }
}
