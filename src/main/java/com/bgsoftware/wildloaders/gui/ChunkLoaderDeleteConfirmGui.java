package com.bgsoftware.wildloaders.gui;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.google.common.collect.ImmutableSet;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Gui;
import me.lucko.helper.text3.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ChunkLoaderDeleteConfirmGui extends Gui {

    private static final ImmutableSet<Integer> NO_ITEM_SLOTS = ImmutableSet.<Integer>builder()
            .add(5, 6, 7, 8)
            .add(14, 15, 16, 17)
            .add(23, 24, 25, 26)
            .build();

    private static final ImmutableSet<Integer> MIDDLE_ITEM_SLOTS = ImmutableSet.<Integer>builder()
            .add(4, 13, 22)
            .build();

    private static final ImmutableSet<Integer> YES_ITEM_SLOTS = ImmutableSet.<Integer>builder()
            .add(0, 1, 2, 3)
            .add(9, 10, 11, 12)
            .add(18, 19, 20, 21)
            .build();

    private final WildLoadersPlugin plugin;
    private final ChunkLoader loader;

    public ChunkLoaderDeleteConfirmGui(Player player, ChunkLoader loader, WildLoadersPlugin plugin) {
        super(player, 3, "Are you sure?");
        this.loader = loader;
        this.plugin = plugin;
    }

    @Override
    public void redraw() {
        NO_ITEM_SLOTS.forEach(slot ->
                setItem(slot, ItemStackBuilder.of(Material.RED_STAINED_GLASS_PANE)
                        .name("&c&lNo")
                        .lore(
                                " ",
                                "&fI want to keep this",
                                "&fChunk Loader",
                                " "
                        )
                        .build(() -> {
                            close();
                            getPlayer().sendMessage(Text.colorize("&aYou have kept your chunk loader."));
                        })));

        MIDDLE_ITEM_SLOTS.forEach(slot ->
                setItem(slot, ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                        .name(" ")
                        .build(null)));

        YES_ITEM_SLOTS.forEach(slot ->
                setItem(slot, ItemStackBuilder.of(Material.LIME_STAINED_GLASS_PANE)
                        .name("&a&lYes")
                        .lore(
                                " ",
                                "&fI want to remove this",
                                "&fChunk Loader",
                                " "
                        )
                        .build(() -> {
                            close();
                            plugin.getLoaders().removeChunkLoader(loader);
                            getPlayer().sendMessage(Text.colorize("&cYour chunk loader has been removed."));
                        })));
    }
}
