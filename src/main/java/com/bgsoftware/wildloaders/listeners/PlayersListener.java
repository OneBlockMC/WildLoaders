package com.bgsoftware.wildloaders.listeners;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@SuppressWarnings("unused")
public final class PlayersListener implements Listener {

    private final WildLoadersPlugin plugin;

    public PlayersListener(WildLoadersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoinMonitor(PlayerJoinEvent event) {
        for (ChunkLoaderNPC npc : plugin.getNPCs().getNPCs().values()) {
            event.getPlayer().hidePlayer(npc.getPlayer());
        }
    }
}
