package com.bgsoftware.wildloaders.island;

import com.bgsoftware.wildloaders.api.WildLoadersAPI;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import org.bukkit.Location;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface IslandChunkLoaderStorageDao {

    default Set<ChunkLoader> getChunkLoadersOnIsland(Island island) {
        return WildLoadersAPI.getWildLoaders()
                .getLoaders()
                .getChunkLoaders()
                .stream()
                .filter(loader -> {
                    Location loaderLocation = loader.getLocation();
                    return island.getBoundingBox().contains(loaderLocation.toVector());
                })
                .collect(Collectors.toSet());
    }

    Optional<String> getCustomLoaderName(ChunkLoaderNPC npc);

    void setCustomLoaderName(ChunkLoaderNPC npc, String name);

    void setup();

    void shutdown();

}
