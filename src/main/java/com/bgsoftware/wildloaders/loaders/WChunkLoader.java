package com.bgsoftware.wildloaders.loaders;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.holograms.Hologram;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import com.bgsoftware.wildloaders.utils.database.Query;
import com.bgsoftware.wildloaders.utils.threads.Executor;
import me.lucko.helper.text3.Text;
import me.lucko.helper.time.DurationFormatter;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;

public final class WChunkLoader implements ChunkLoader {

    private static final WildLoadersPlugin plugin = WildLoadersPlugin.getPlugin();

    private final UUID whoPlaced;
    private final Location location;
    private final Chunk[] loadedChunks;
    private final String loaderName;
    private final ITileEntityChunkLoader tileEntityChunkLoader;

    private boolean active = true;
    private boolean waiting = false;
    private long timeLeft;

    private final List<Hologram> holograms;

    public WChunkLoader(LoaderData loaderData, UUID whoPlaced, Location location, long timeLeft) {
        this.loaderName = loaderData.getName();
        this.whoPlaced = whoPlaced;
        this.location = location.clone();
        this.loadedChunks = calculateChunks(loaderData, whoPlaced, this.location);
        this.timeLeft = timeLeft;
        this.tileEntityChunkLoader = plugin.getNMSAdapter().createLoader(this);
        this.holograms = tileEntityChunkLoader.getHolograms().stream().toList();
    }

    @Override
    public LoaderData getLoaderData() {
        return plugin.getLoaders().getLoaderData(loaderName).orElse(null);
    }

    @Override
    public OfflinePlayer getWhoPlaced() {
        return Bukkit.getOfflinePlayer(whoPlaced);
    }

    public boolean isNotActive() {
        if (active)
            active = plugin.getLoaders().getChunkLoader(getLocation()).orElse(null) == this;
        return !active;
    }

    @Override
    public long getTimeLeft() {
        return timeLeft;
    }

    @Override
    public void setTimeLeft(long provided) {
        this.timeLeft = provided;
    }

    public void tick() {
//        if (!(timeLeft < 0)) {
//            System.out.println("ticking");
//            plugin.getProviders().tick(loadedChunks);
//        }

        if (!isInfinite()) {
            timeLeft--;
            if (timeLeft <= 0 && !waiting) {
                waiting = true;
                getNPC().ifPresent(ChunkLoaderNPC::die);
                plugin.getLoaders().unloadLoadedChunks(this);
                holograms.get(1).setHologramName(Text.colorize("&cThis chunk loader has run out of time."));
                holograms.get(0).setHologramName(Text.colorize("&cPurchase more time for this chunk loader to activate."));
            } else if (!waiting && timeLeft % 10 == 0) {
                Query.UPDATE_CHUNK_LOADER_TIME_LEFT.insertParameters()
                        .setObject(timeLeft)
                        .setLocation(location)
                        .queue(location);
            }
        }
    }

    public boolean isInfinite() {
        return timeLeft == Integer.MIN_VALUE;
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public Chunk[] getLoadedChunks() {
        return loadedChunks;
    }

    @Override
    public Optional<ChunkLoaderNPC> getNPC() {
        return plugin.getNPCs().getNPC(location);
    }

    @Override
    public void remove() {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(this::remove);
            return;
        }

        plugin.getNMSAdapter().removeLoader(this, timeLeft <= 0 || isNotActive());
        plugin.getLoaders().removeChunkLoader(this);

        getLocation().getBlock().setType(Material.AIR);
    }

    @Override
    public ItemStack getLoaderItem() {
        return getLoaderData().getLoaderItem(getTimeLeft());
    }

    @Override
    public Collection<Hologram> getHolograms() {
        return holograms;
    }

    @Override
    public boolean isWaiting() {
        return waiting;
    }

    @Override
    public void setWaiting(boolean value) {
        this.waiting = value;
    }

    public List<String> getHologramLines() {
        return isInfinite() ? plugin.getSettings().infiniteHologramLines : plugin.getSettings().hologramLines;
    }

    private static Chunk[] calculateChunks(LoaderData loaderData, UUID whoPlaced, Location original) {
        List<Chunk> chunkList = new ArrayList<>();

        if (loaderData.isChunksSpread()) {
            calculateClaimChunks(original.getChunk(), whoPlaced, chunkList);
        }

        if (chunkList.isEmpty()) {
            int chunkX = original.getBlockX() >> 4, chunkZ = original.getBlockZ() >> 4;

            for (int x = -loaderData.getChunksRadius(); x <= loaderData.getChunksRadius(); x++)
                for (int z = -loaderData.getChunksRadius(); z <= loaderData.getChunksRadius(); z++)
                    chunkList.add(original.getWorld().getChunkAt(chunkX + x, chunkZ + z));
        }

        return chunkList.toArray(new Chunk[0]);
    }

    private static void calculateClaimChunks(Chunk originalChunk, UUID whoPlaced, List<Chunk> chunkList) {
        if (!plugin.getProviders().hasChunkAccess(whoPlaced, originalChunk))
            return;

        chunkList.add(originalChunk);

        int chunkX = originalChunk.getX(), chunkZ = originalChunk.getZ();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) // We don't want to add the originalChunk again.
                    calculateClaimChunks(originalChunk.getWorld().getChunkAt(chunkX + x, chunkZ + z), whoPlaced, chunkList);
            }
        }

    }
}
