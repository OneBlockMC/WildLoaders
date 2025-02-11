package com.bgsoftware.wildloaders.nms.v1182.loader;

import com.bgsoftware.wildloaders.api.ChunkLoaderMetaDao;
import com.bgsoftware.wildloaders.api.holograms.Hologram;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import com.bgsoftware.wildloaders.loaders.ITileEntityChunkLoader;
import com.bgsoftware.wildloaders.loaders.WChunkLoader;
import com.bgsoftware.wildloaders.nms.v1182.EntityHologram;
import me.lucko.helper.Helper;
import me.lucko.helper.Services;
import me.lucko.helper.text3.Text;
import me.lucko.helper.time.DurationFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChunkLoaderBlockEntity extends BlockEntity implements ITileEntityChunkLoader {

    public static final Map<Long, ChunkLoaderBlockEntity> chunkLoaderBlockEntityMap = new HashMap<>();

    public final List<EntityHologram> holograms = new ArrayList<>();
    private final WChunkLoader chunkLoader;
    private final Block loaderBlock;
    private final ChunkLoaderBlockEntityTicker ticker;
    private final ServerLevel serverLevel;
    private final BlockPos blockPos;

    private short currentTick = 20;
    public boolean removed = false;

    public ChunkLoaderBlockEntity(ChunkLoader chunkLoader, ServerLevel serverLevel, BlockPos blockPos) {
        super(BlockEntityType.COMMAND_BLOCK, blockPos, serverLevel.getBlockState(blockPos));

        this.chunkLoader = (WChunkLoader) chunkLoader;
        this.ticker = new ChunkLoaderBlockEntityTicker(this);
        this.blockPos = blockPos;
        this.serverLevel = serverLevel;

        setLevel(serverLevel);

        loaderBlock = serverLevel.getBlockState(blockPos).getBlock();

        long chunkPosLong = ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        chunkLoaderBlockEntityMap.put(chunkPosLong, this);

        List<String> hologramLines = this.chunkLoader.getHologramLines();

        double currentY = blockPos.getY() + 1;
        for (int i = hologramLines.size(); i > 0; i--) {
            EntityHologram hologram = new EntityHologram(serverLevel, blockPos.getX() + 0.5, currentY, blockPos.getZ() + 0.5);
            updateName(hologram, hologramLines.get(i - 1));
            serverLevel.addFreshEntity(hologram);
            currentY += 0.23;
            holograms.add(hologram);
        }
    }

    public void tick() {
        if (removed || ++currentTick <= 20)
            return;

        currentTick = 0;

        if (chunkLoader.isNotActive() || this.serverLevel.getBlockState(this.blockPos).getBlock() != loaderBlock) {
            chunkLoader.remove();
            return;
        }

        chunkLoader.tick();

        if (chunkLoader.isInfinite() || chunkLoader.isWaiting()) {
            return;
        }

        List<String> hologramLines = chunkLoader.getHologramLines();
        int hologramsAmount = holograms.size();

        for (int i = hologramsAmount; i > 0; i--) {
            EntityHologram hologram = holograms.get(hologramsAmount - i);
            updateName(hologram, hologramLines.get(i - 1));
        }
    }

    @Override
    public Collection<Hologram> getHolograms() {
        return Collections.unmodifiableList(holograms);
    }

    @Override
    public boolean isRemoved() {
        return removed || super.isRemoved();
    }

    public ChunkLoaderBlockEntityTicker getTicker() {
        return ticker;
    }

    private void updateName(EntityHologram hologram, String line) {
        ChunkLoaderMetaDao dao = Services.load(ChunkLoaderMetaDao.class);
        ChunkLoaderNPC npc = chunkLoader.getNPC().orElse(null);
        if (npc == null) {
            return;
        }

        hologram.setHologramName(line
                .replace("{0}", dao.getCustomLoaderName(npc)
                        .map(customName -> Text.colorize("&b" + customName + " &7(Owned by " + chunkLoader.getWhoPlaced().getName() + ")"))
                        .orElse(Text.colorize("&b" + chunkLoader.getWhoPlaced().getName() + "'s Chunk Loader")))
                .replace("{1}", DurationFormatter.format(Duration.ofSeconds(chunkLoader.getTimeLeft()), true))
        );
    }

}

