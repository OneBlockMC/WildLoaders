package com.bgsoftware.wildloaders.gui;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.island.IslandChunkLoaderStorageDao;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Gui;
import me.lucko.helper.text3.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ChunkLoaderManageGui extends Gui {

    private final IslandChunkLoaderStorageDao dao;
    private final WildLoadersPlugin plugin;
    private final ChunkLoader loader;
    private final Economy economy;

    public ChunkLoaderManageGui(Player player, IslandChunkLoaderStorageDao dao, WildLoadersPlugin plugin, ChunkLoader loader, Economy economy) {
        super(player, 3, "&dManage Chunk Loaders");
        this.dao = dao;
        this.loader = loader;
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public void redraw() {
        setItem(11, ItemStackBuilder.of(Material.NAME_TAG)
                .name("&aRename")
                .lore(
                        "Click here to rename",
                        "your Chunk Loader!"
                )
                .build(() -> {
                    //TODO
                }));

        setItem(13, ItemStackBuilder.of(Material.GOLD_INGOT)
                .name("&6Buy Time")
                .lore(
                        "&fClick to buy more time for",
                        "&fyour Chunk Loader!"
                )
                .build(() -> {
                    if (loader.getTimeLeft() >= ChunkLoader.MAX_ACTIVITY_TIME.toSeconds()) {
                        getPlayer().sendMessage(Text.colorize("&cYour chunk loader is already at max time capacity!"));
                        return;
                    }

                    close();
                    new ChunkLoaderTimeGui(getPlayer(), loader, economy).open();
                }));

        setItem(15, ItemStackBuilder.of(Material.BARRIER)
                .name("&cDelete Chunk Loader")
                .lore(
                        "&fClick here to delete",
                        "&fyour Chunk Loader!"
                )
                .build(() -> {
                    close();
                    new ChunkLoaderDeleteConfirmGui(getPlayer(), loader, plugin).open();
                }));
    }
}
