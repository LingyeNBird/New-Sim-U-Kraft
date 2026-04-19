package com.xiaoliang.simukraft.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CityNameCache {
    private static final Map<UUID, String> cityNames = new HashMap<>();

    public static void put(UUID cityId, String cityName) {
        cityNames.put(cityId, cityName);
    }

    public static String get(UUID cityId) {
        return cityNames.get(cityId);
    }

    public static boolean contains(UUID cityId) {
        return cityNames.containsKey(cityId);
    }

    public static void clear() {
        cityNames.clear();
    }
}