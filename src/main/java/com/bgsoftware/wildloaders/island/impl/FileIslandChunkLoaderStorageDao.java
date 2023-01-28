package com.bgsoftware.wildloaders.island.impl;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.island.IslandChunkLoaderStorageDao;
import com.google.common.base.Splitter;
import me.lucko.helper.gson.GsonProvider;
import me.lucko.helper.serialize.FileStorageHandler;
import me.lucko.helper.serialize.Position;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileIslandChunkLoaderStorageDao extends FileStorageHandler<Map<Position, String>> implements IslandChunkLoaderStorageDao {

    private static final Splitter SPLITTER = Splitter.on('=');

    private final Map<Position, String> loaderNameMap = new IdentityHashMap<>();

    public FileIslandChunkLoaderStorageDao(WildLoadersPlugin plugin) {
        super("chunk_loader_names", ".json", plugin.getDataFolder());
    }

    @Override
    public Optional<String> getCustomLoaderName(Location location) {
        return Optional.ofNullable(loaderNameMap.get(Position.of(location)));
    }

    @Override
    public void setCustomLoaderName(ChunkLoader loader, String name) {
        loaderNameMap.put(Position.of(loader.getLocation()), ChatColor.stripColor(name));
    }

    @Override
    public void setup() {
        Optional<Map<Position, String>> optional = load();
        if (optional.isEmpty()) {
            saveAndBackup(loaderNameMap);
        } else {
            loaderNameMap.putAll(optional.get());
        }
    }

    @Override
    public void shutdown() {
        saveAndBackup(loaderNameMap);
    }

    @Override
    @NotNull
    protected Map<Position, String> readFromFile(@NotNull Path path) {
        Map<Position, String> dataMap = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            // read all the data from the file.
            reader.lines().forEach(line -> {
                Iterator<String> it = SPLITTER.split(line).iterator();
                dataMap.put(GsonProvider.standard().fromJson(it.next(), Position.class), it.next());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataMap;
    }

    @Override
    protected void saveToFile(@NotNull Path path, Map<Position, String> map) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Map.Entry<Position, String> entry : map.entrySet()) {
                writer.write(GsonProvider.standard().toJson(entry.getKey()) + "=" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
