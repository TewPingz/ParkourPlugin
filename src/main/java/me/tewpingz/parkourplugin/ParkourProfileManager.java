package me.tewpingz.parkourplugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
public class ParkourProfileManager {

    private Map<UUID, ParkourProfile> profileMap;

    public ParkourProfileManager() {
        this.profileMap = new HashMap<>();
    }

    public ParkourProfile getProfile(UUID uuid) {
        return this.profileMap.get(uuid);
    }

    public void addProfile(UUID uuid) {
        this.profileMap.put(uuid, new ParkourProfile(uuid));
    }

    public void removeProfile(UUID uuid) {
        this.profileMap.remove(uuid);
    }

    @Data
    @RequiredArgsConstructor
    public static class ParkourProfile {

        private final UUID playerUUID;

        private long startTime = -1;
        private ParkourRoute.ParkourCheckpoint expectedCheckpoint = null;

    }
}
