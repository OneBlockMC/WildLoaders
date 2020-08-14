package com.bgsoftware.wildloaders.handlers;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.managers.LoadersManager;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.loaders.WChunkLoader;
import com.bgsoftware.wildloaders.loaders.WLoaderData;
import com.bgsoftware.wildloaders.utils.chunks.ChunkPosition;
import com.google.common.collect.Maps;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LoadersHandler implements LoadersManager {

    private final Map<Location, ChunkLoader> chunkLoaders = Maps.newConcurrentMap();
    private final Map<ChunkPosition, ChunkLoader> chunkLoadersByChunks = Maps.newConcurrentMap();
    private final Map<String, LoaderData> loadersData = Maps.newConcurrentMap();
    private final WildLoadersPlugin plugin;

    public LoadersHandler(WildLoadersPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public Optional<ChunkLoader> getChunkLoader(Chunk chunk) {
        return Optional.ofNullable(chunkLoadersByChunks.get(ChunkPosition.of(chunk)));
    }

    @Override
    public Optional<ChunkLoader> getChunkLoader(Location location) {
        return Optional.ofNullable(chunkLoaders.get(location));
    }

    @Override
    public List<ChunkLoader> getChunkLoaders() {
        return Collections.unmodifiableList(new ArrayList<>(chunkLoaders.values()));
    }

    @Override
    public Optional<LoaderData> getLoaderData(String name) {
        return Optional.ofNullable(loadersData.get(name));
    }

    @Override
    public List<LoaderData> getLoaderDatas() {
        return new ArrayList<>(loadersData.values());
    }

    @Override
    public ChunkLoader addChunkLoader(LoaderData loaderData, Player whoPlaced, Location location, long timeLeft) {
        ChunkLoader chunkLoader = new WChunkLoader(loaderData.getName(), whoPlaced, location, timeLeft);
        chunkLoaders.put(location, chunkLoader);
        chunkLoadersByChunks.put(ChunkPosition.of(location), chunkLoader);
        plugin.getNPCs().createNPC(location);
        return chunkLoader;
    }

    @Override
    public void removeChunkLoader(ChunkLoader chunkLoader) {
        Location location = chunkLoader.getLocation();
        chunkLoaders.remove(location);
        chunkLoadersByChunks.remove(ChunkPosition.of(location));
        chunkLoader.getNPC().ifPresent(npc -> plugin.getNPCs().killNPC(npc));
    }

    @Override
    public void createLoaderData(String name, long timeLeft, ItemStack itemStack) {
        LoaderData loaderData = new WLoaderData(name, timeLeft, itemStack);
        loadersData.put(name, loaderData);
    }

    @Override
    public void removeLoadersData() {
        loadersData.clear();
    }

    @Override
    public void removeChunkLoaders() {
        chunkLoaders.clear();
        chunkLoadersByChunks.clear();
    }
}
