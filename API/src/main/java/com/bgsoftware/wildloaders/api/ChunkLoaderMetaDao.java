package com.bgsoftware.wildloaders.api;

import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;

import java.util.Optional;

public interface ChunkLoaderMetaDao {

    Optional<String> getCustomLoaderName(ChunkLoaderNPC npc);

    void setCustomLoaderName(ChunkLoaderNPC npc, String name);

    void setup();

    void shutdown();

}
