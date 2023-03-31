package com.bgsoftware.wildloaders.gui;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Gui;
import me.lucko.helper.menu.scheme.MenuScheme;
import me.lucko.helper.menu.scheme.StandardSchemeMappings;
import me.lucko.helper.serialize.Position;
import me.lucko.helper.text3.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ChunkLoaderManageGui extends Gui {

    private static final MenuScheme PANE_SCHEME = new MenuScheme(StandardSchemeMappings.STAINED_GLASS)
            .mask("111111111")
            .mask("110101011")
            .mask("111111111")
            .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0)
            .scheme(0, 0, 0, 0, 0, 0)
            .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0);

    private final WildLoadersPlugin plugin;
    private final ChunkLoader loader;
    private final Economy economy;

    public ChunkLoaderManageGui(Player player, WildLoadersPlugin plugin, ChunkLoader loader, Economy economy) {
        super(player, 3, "&dManage Chunk Loader");
        this.loader = loader;
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public void redraw() {
        PANE_SCHEME.apply(this);

        setItem(11, ItemStackBuilder.of(Material.NAME_TAG)
                .name("&aRename")
                .lore(
                        " ",
                        "&fClick here to rename",
                        "&fyour Chunk Loader!",
                        " "
                )
                .build(() -> {
                    close();
                    plugin.getRenameCache().put(getPlayer().getUniqueId(), Position.of(loader.getLocation()));
                    getPlayer().sendMessage(Text.colorize("&a&oPlease enter a name for your chunk loader."));
                }));

        setItem(13, ItemStackBuilder.of(Material.GOLD_INGOT)
                .name("&6Buy Time")
                .lore(
                        " ",
                        "&fClick to buy more time for",
                        "&fyour Chunk Loader!",
                        " "
                )
                .build(() -> {
                    if (loader.getTimeLeft() >= ChunkLoader.MAX_ACTIVITY_TIME.toSeconds()) {
                        getPlayer().sendMessage(Text.colorize("&cYour chunk loader is already at max time capacity!"));
                        return;
                    }

                    close();
                    new ChunkLoaderTimeGui(getPlayer(), loader, economy, plugin).open();
                }));

        setItem(15, ItemStackBuilder.of(Material.BARRIER)
                .name("&cDelete Chunk Loader")
                .lore(
                        " ",
                        "&fClick here to delete",
                        "&fyour Chunk Loader!",
                        " ",
                        "&cYou will not get the",
                        "&cChunk Loader back."
                )
                .build(() -> {
                    close();
                    new ChunkLoaderDeleteConfirmGui(getPlayer(), loader, plugin).open();
                }));
    }
}
