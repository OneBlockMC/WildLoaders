package com.bgsoftware.wildloaders.listeners;

import com.bgsoftware.wildloaders.Locale;
import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.ChunkLoaderMetaDao;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.gui.PaginatedChunkLoaderListGui;
import com.bgsoftware.wildloaders.utils.chunks.ChunkPosition;
import com.bgsoftware.wildloaders.utils.legacy.Materials;
import me.lucko.helper.serialize.BlockPosition;
import me.lucko.helper.text3.Text;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public final class BlocksListener implements Listener {

    private final WildLoadersPlugin plugin;
    private final Economy economy;
    private final ChunkLoaderMetaDao dao;

    private static final List<String> REPLY_MESSAGES = List.of(
            "&cAre you sure you want to place the Chunk Loader here?",
            " ",
            "&8* &fYou will not be able to get the Chunk Loader back once placed.",
            " ",
            "&aPlace again to confirm!"
    );

    private final ExpiringMap<UUID, BlockPosition> placementCacheMap = ExpiringMap
            .builder()
            .expirationPolicy(ExpirationPolicy.CREATED)
            .expiration(15L, TimeUnit.SECONDS)
            .asyncExpirationListener((uuid, $) -> {
                Player target = Bukkit.getPlayer(((UUID) uuid));
                if (target == null) return;
                target.sendMessage(Text.colorize("&cYour chunk loader placement time has expired."));
            })
            .build();

    public BlocksListener(WildLoadersPlugin plugin, Economy economy, ChunkLoaderMetaDao dao) {
        this.plugin = plugin;
        this.economy = economy;
        this.dao = dao;
    }

    @EventHandler
    private void onItemSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!placementCacheMap.containsKey(player.getUniqueId())) return;
        placementCacheMap.remove(player.getUniqueId());
        player.sendMessage(Text.colorize("&cYour chunk loader placement activity has been invalidated."));
    }

    @EventHandler
    private void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!placementCacheMap.containsKey(player.getUniqueId())) return;
        placementCacheMap.remove(player.getUniqueId());
        player.sendMessage(Text.colorize("&cYour chunk loader placement activity has been invalidated."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onLoaderPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        String loaderName = plugin.getNMSAdapter().getTag(e.getItemInHand(), "loader-name", "");
        Optional<LoaderData> optionalLoaderData = plugin.getLoaders().getLoaderData(loaderName);

        if (optionalLoaderData.isEmpty())
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
        int currentLoaderCount = plugin.getChunkLoadersOnIsland(island).size();
        if (currentLoaderCount >= plugin.getChunkLoaderLimit()) {
            e.setCancelled(true);
            player.sendMessage(Text.colorize("&cYour island is currently at the chunk loader limit of " + currentLoaderCount + "."));
            return;
        }

        if (!placementCacheMap.containsKey(player.getUniqueId())) {
            e.setCancelled(true);
            placementCacheMap.put(player.getUniqueId(), BlockPosition.of(block));
            REPLY_MESSAGES.stream()
                    .map(Text::colorize)
                    .forEach(player::sendMessage);
            return;
        }


        BlockPosition position = placementCacheMap.get(player.getUniqueId());
        if (!position.equals(BlockPosition.of(block))) {
            e.setCancelled(true);
            player.sendMessage(Text.colorize("&cPlease place your chunk loader in the same location!"));
            return;
        }

        placementCacheMap.remove(player.getUniqueId());
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
