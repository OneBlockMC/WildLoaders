package com.bgsoftware.wildloaders.handlers;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.managers.NPCManager;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import com.bgsoftware.wildloaders.npc.NPCIdentifier;
import com.bgsoftware.wildloaders.utils.database.Query;
import com.google.common.collect.Maps;
import me.lucko.helper.serialize.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class NPCHandler implements NPCManager {

    private static int NPCS_COUNTER = 0;

    private final WildLoadersPlugin plugin;
    private final Map<NPCIdentifier, ChunkLoaderNPC> npcs = Maps.newConcurrentMap();
    private final Map<NPCIdentifier, UUID> npcUUIDs = Maps.newConcurrentMap();

    public NPCHandler(WildLoadersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<ChunkLoaderNPC> getNPC(Location location) {
        return Optional.ofNullable(npcs.get(new NPCIdentifier(location)));
    }

    @Override
    public ChunkLoaderNPC createNPC(Location location) {
        NPCIdentifier identifier = new NPCIdentifier(location);

        if (npcs.containsKey(identifier)) {
            ChunkLoaderNPC npc = npcs.get(identifier);
            npc.spawn();
            return npc;
        }

        return npcs.computeIfAbsent(new NPCIdentifier(location), i -> plugin.getNMSAdapter().createNPC(i.getSpawnLocation(), getUUID(i)));
    }

    @Override
    public boolean isNPC(LivingEntity livingEntity) {
        ChunkLoaderNPC npc = getNPC(livingEntity.getLocation()).orElse(null);
        return npc != null && npc.getUniqueId().equals(livingEntity.getUniqueId());
    }

    @Override
    public void killNPC(ChunkLoaderNPC npc) {
        NPCIdentifier identifier = new NPCIdentifier(npc.getLocation());
        npcs.remove(identifier);

        npcUUIDs.remove(identifier);

        Query.DELETE_NPC_IDENTIFIER.insertParameters()
                .setLocation(identifier.getSpawnLocation())
                .queue(npc.getUniqueId());

        npc.die();
    }

    @Override
    public void killAllNPCs() {
        for (ChunkLoaderNPC npc : npcs.values()) {
            npc.die();
        }

        npcs.clear();
    }

    public Map<NPCIdentifier, ChunkLoaderNPC> getNPCs() {
        return Collections.unmodifiableMap(npcs);
    }

    public void registerUUID(Location location, UUID uuid) {
        npcUUIDs.put(new NPCIdentifier(location), uuid);
    }

    private UUID getUUID(NPCIdentifier identifier) {
        if (npcUUIDs.containsKey(identifier))
            return npcUUIDs.get(identifier);

        UUID uuid;

        do {
            uuid = UUID.randomUUID();
        } while (npcUUIDs.containsValue(uuid));

        npcUUIDs.put(identifier, uuid);

        Query.INSERT_NPC_IDENTIFIER.insertParameters()
                .setLocation(identifier.getSpawnLocation())
                .setObject(uuid.toString())
                .queue(uuid);

        return uuid;
    }

    public static String getName(String worldName) {
        return "Loader-" + (worldName.length() > 7 ? worldName.substring(0, 7) : worldName) + "-" + (NPCS_COUNTER++);
    }

}
