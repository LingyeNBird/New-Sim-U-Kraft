package com.xiaoliang.simukraft.employment.service;

import com.xiaoliang.simukraft.employment.persistence.JsonEmploymentRepository;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.WeakHashMap;

public final class EmploymentServices {
    private static final Map<MinecraftServer, DefaultEmploymentService> SERVICES = new WeakHashMap<>();

    private EmploymentServices() {
    }

    public static synchronized DefaultEmploymentService get(MinecraftServer server) {
        return SERVICES.computeIfAbsent(server, s -> new DefaultEmploymentService(new JsonEmploymentRepository(s)));
    }

    public static synchronized void clear(MinecraftServer server) {
        SERVICES.remove(server);
    }
}

