package com.bgsoftware.wildloaders.meta;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import com.bgsoftware.wildloaders.api.ChunkLoaderMetaDao;
import com.google.common.base.Splitter;
import me.lucko.helper.serialize.FileStorageHandler;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileChunkLoaderMetaStorage extends FileStorageHandler<Map<UUID, String>> implements ChunkLoaderMetaDao {

    private static final Splitter SPLITTER = Splitter.on('=');

    private final Map<UUID, String> loaderNameMap = new HashMap<>();

    public FileChunkLoaderMetaStorage(WildLoadersPlugin plugin) {
        super("chunk_loader_names", ".json", plugin.getDataFolder());
    }

    @Override
    public Optional<String> getCustomLoaderName(ChunkLoaderNPC npc) {
        return Optional.ofNullable(loaderNameMap.get(npc.getUniqueId())).map(input -> WildLoadersPlugin.STRIP_COLOR_PATTERN.matcher(input).replaceAll(""));
    }

    @Override
    public void setCustomLoaderName(ChunkLoaderNPC npc, String name) {
        loaderNameMap.put(npc.getUniqueId(), name);
    }

    @Override
    public void setup() {
        Optional<Map<UUID, String>> optional = load();
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
    protected Map<UUID, String> readFromFile(@NotNull Path path) {
        Map<UUID, String> dataMap = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            // read all the data from the file.
            reader.lines().forEach(line -> {
                Iterator<String> it = SPLITTER.split(line).iterator();
                dataMap.put(UUID.fromString(it.next()), it.next());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataMap;
    }

    @Override
    protected void saveToFile(@NotNull Path path, Map<UUID, String> map) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Map.Entry<UUID, String> entry : map.entrySet()) {
                writer.write(entry.getKey().toString() + "=" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
