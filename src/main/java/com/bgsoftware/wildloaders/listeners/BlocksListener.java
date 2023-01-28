package com.bgsoftware.wildloaders.listeners;

import com.bgsoftware.wildloaders.Locale;
import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.gui.PaginatedChunkLoaderListGui;
import com.bgsoftware.wildloaders.island.IslandChunkLoaderStorageDao;
import com.bgsoftware.wildloaders.utils.chunks.ChunkPosition;
import com.bgsoftware.wildloaders.utils.legacy.Materials;
import me.lucko.helper.text3.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.libs.org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;

@SuppressWarnings("unused")
public final class BlocksListener implements Listener {

    private final WildLoadersPlugin plugin;
    private final Economy economy;
    private final IslandChunkLoaderStorageDao dao;

    public BlocksListener(WildLoadersPlugin plugin, Economy economy, IslandChunkLoaderStorageDao dao) {
        this.plugin = plugin;
        this.economy = economy;
        this.dao = dao;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onLoaderPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        String loaderName = plugin.getNMSAdapter().getTag(e.getItemInHand(), "loader-name", "");
        Optional<LoaderData> optionalLoaderData = plugin.getLoaders().getLoaderData(loaderName);

        if (!optionalLoaderData.isPresent())
            return;

        if (!player.hasPermission("wildloaders.use")) {
            e.setCancelled(true);
            Locale.NO_PLACE_PERMISSION.send(player);
            return;
        }

        if (plugin.getLoaders().getChunkLoader(block.getLocation().getChunk()).isPresent()) {
            e.setCancelled(true);
            Locale.ALREADY_LOADED.send(player);
            return;
        }

        Optional<Island> optional = BentoBox.getInstance()
                .getIslands()
                .getIslandAt(player.getLocation());
        if (optional.isEmpty()) {
            e.setCancelled(true);
            player.sendMessage(Text.colorize("&cYou can only place Chunk Loaders on islands."));
            return;
        }

        Island island = optional.get();
        int currentLoaderCount = dao.getChunkLoadersOnIsland(island).size();
        if (currentLoaderCount >= plugin.getChunkLoaderLimit()) {
            e.setCancelled(true);
            player.sendMessage(Text.colorize("&cYour island is currently at the chunk loader limit of " + currentLoaderCount + "."));
            return;
        }

        LoaderData loaderData = optionalLoaderData.get();

        long timeLeft = plugin.getNMSAdapter().getTag(e.getItemInHand(), "loader-time", loaderData.getTimeLeft());

        plugin.getLoaders().addChunkLoader(loaderData, player, block.getLocation(), timeLeft);

        Locale.PLACED_LOADER.send(e.getPlayer(), ChunkPosition.of(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onLoaderBreak(BlockBreakEvent e) {
        if (plugin.getLoaders().getChunkLoader(e.getBlock().getLocation()).isPresent()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Text.colorize("&cPlease remove chunk loaders through the chunk loader menu."));
        }

//        if (handleLoaderBreak(e.getBlock(), e.getPlayer().getGameMode() != GameMode.CREATIVE))
//            Locale.BROKE_LOADER.send(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onLoaderExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(block -> handleLoaderBreak(block, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onSpawnerPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() != Materials.SPAWNER.toBukkitType())
            return;

        if (!plugin.getLoaders().getChunkLoader(e.getBlock().getChunk()).isPresent())
            return;

        plugin.getNMSAdapter().updateSpawner(e.getBlock().getLocation(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onLoaderInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block != null) {
            Action action = event.getAction();

            Optional<Island> optional = BentoBox.getInstance()
                    .getIslands()
                    .getIslandAt(block.getLocation());
            if (optional.isEmpty()) {
                return;
            }

            Island island = optional.get();

            plugin.getLoaders()
                    .getChunkLoader(block.getLocation())
                    .ifPresent(loader -> {
                        event.setCancelled(true);
                        new PaginatedChunkLoaderListGui(island, dao, plugin, economy, player).open();
                    });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onLoaderPistonRetract(BlockPistonRetractEvent e) {
        try {
            for (Block block : e.getBlocks()) {
                if (plugin.getLoaders().getChunkLoader(block.getLocation()).isPresent()) {
                    e.setCancelled(true);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onLoaderPistonExtend(BlockPistonExtendEvent e) {
        try {
            for (Block block : e.getBlocks()) {
                if (plugin.getLoaders().getChunkLoader(block.getLocation()).isPresent()) {
                    e.setCancelled(true);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean handleLoaderBreak(Block block, boolean dropItem) {
        Location blockLoc = block.getLocation();
        Optional<ChunkLoader> optionalChunkLoader = plugin.getLoaders().getChunkLoader(blockLoc);

        if (!optionalChunkLoader.isPresent())
            return false;

        ChunkLoader chunkLoader = optionalChunkLoader.get();
        chunkLoader.remove();

        if (dropItem)
            blockLoc.getWorld().dropItemNaturally(blockLoc, chunkLoader.getLoaderItem());

        return true;
    }

}
