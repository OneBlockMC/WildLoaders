package com.bgsoftware.wildloaders.api.loaders;

import com.bgsoftware.wildloaders.api.holograms.Hologram;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;

public interface ChunkLoader {

    Duration MAX_ACTIVITY_TIME = Duration.of(3L, ChronoUnit.DAYS);

    /**
     * Get the loader data from of chunk loader.
     */
    LoaderData getLoaderData();

    /**
     * Get the player who placed the chunk loader.
     */
    OfflinePlayer getWhoPlaced();

    /**
     * Get the amount of time that is left until the chunk loader finishes, in ticks.
     */
    long getTimeLeft();

    /**
     * Set the amount of time that is left until the chunk loader finishes, in ticks.
     */
    void setTimeLeft(long ticks);


    /**
     * Get the location of the chunk loader.
     */
    Location getLocation();

    /**
     * Get the chunks that this chunk-loader is loading.
     */
    Chunk[] getLoadedChunks();

    /**
     * Get the NPC of this chunk loader.
     */
    Optional<ChunkLoaderNPC> getNPC();

    /**
     * Remove this chunk loader.
     */
    void remove();

    /**
     * Get the drop item of this chunk loader.
     */
    ItemStack getLoaderItem();

    /**
     * Get the holograms of the chunk loader.
     */
    Collection<Hologram> getHolograms();

    boolean isWaiting();

    void setWaiting(boolean value);


}
