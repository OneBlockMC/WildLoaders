package com.bgsoftware.wildloaders.gui;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.island.IslandChunkLoaderStorageDao;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.paginated.PaginatedGui;
import me.lucko.helper.menu.paginated.PaginatedGuiBuilder;
import me.lucko.helper.menu.scheme.MenuScheme;
import me.lucko.helper.menu.scheme.StandardSchemeMappings;
import me.lucko.helper.serialize.BlockPosition;
import me.lucko.helper.text3.Text;
import me.lucko.helper.time.DurationFormatter;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.database.objects.Island;

import java.time.Duration;
import java.util.stream.Collectors;

public class PaginatedChunkLoaderListGui extends PaginatedGui {

    public PaginatedChunkLoaderListGui(Island island, IslandChunkLoaderStorageDao dao, WildLoadersPlugin plugin, Economy economy, Player player) {
        super(gui -> {
                    gui.setItem(40, ItemStackBuilder.of(Material.GOLD_INGOT)
                            .name("&b&lBuy Chunk Loaders")
                            .lore(
                                    " ",
                                    "&6&lClick &fto buy"
                            )
                            .build(() -> new ChunkLoaderPurchaseGui(player, island, plugin, economy, dao).open()));

                    return dao.getChunkLoadersOnIsland(island)
                            .stream()
                            .sorted((first, second) -> (int) (second.getTimeLeft() - first.getTimeLeft()))
                            .map(chunkLoader -> {
                                BlockPosition position = BlockPosition.of(chunkLoader.getLocation());

                                return chunkLoader.getNPC()
                                        .map(npc ->
                                                ItemStackBuilder.of(Material.BEACON)
                                                        .name(dao.getCustomLoaderName(npc).orElse("Chunk Loader"))
                                                        .lore(
                                                                " ",
                                                                "&f&l* &7Time Left&f: " + (chunkLoader.getTimeLeft() <= 0 ? "&cTime Expired" : DurationFormatter.format(Duration.ofSeconds(chunkLoader.getTimeLeft()), true)),
                                                                "&f&l* &7Status&f: " + (chunkLoader.getTimeLeft() <= 0 ? "&cDisabled" : "&aEnabled"),
                                                                "&f&l* &7Location&f: " + WordUtils.capitalizeFully(position.getWorld().replace("_", " ")) + " @ " + Math.round(position.getX()) + ", " + Math.round(position.getY()) + ", " + Math.round(position.getZ()),
                                                                " ",
                                                                "&f&l* &e&oLeft-Click to teleport",
                                                                "&f&l* &e&oRight-Click to make changes",
                                                                " "
                                                        )
                                                        .build(() -> {
                                                            gui.close();
                                                            new ChunkLoaderManageGui(player, plugin, chunkLoader, economy).open();
                                                        }, () -> {
                                                            gui.close();
                                                            player.teleport(position.toLocation().add(0, 1, 0));
                                                            player.sendMessage(Text.colorize("&a&oTeleporting you to the chunk loader..."));
                                                        }))
                                        .orElse(ItemStackBuilder.of(Material.BARRIER)
                                                .name("&4&lError Processing Chunk Loader")
                                                .lore(
                                                        " ",
                                                        "&cThe chunk loader info could not",
                                                        "&cbe loaded at this time. Please contact",
                                                        "&ca staff member if this issue persists.",
                                                        " "
                                                ).build(null));
                            })
                            .collect(Collectors.toList());
                },

                player,

                PaginatedGuiBuilder
                        .create()

                        .nextPageSlot(new MenuScheme()
                                .maskEmpty(5)
                                .mask("000001000")
                                .getMaskedIndexes()
                                .get(0))

                        .previousPageSlot(new MenuScheme()
                                .maskEmpty(5)
                                .mask("000100000")
                                .getMaskedIndexes()
                                .get(0))

                        .lines(PaginatedGuiBuilder.DEFAULT_LINES)

                        .nextPageItem(info -> ItemStackBuilder.of(Material.LEVER)
                                .name("&aNext Page &f(" + info.getCurrent() + "/" + info.getSize() + ")")
                                .lore("&7&oClick to advance.")
                                .build())

                        .previousPageItem(info -> ItemStackBuilder.of(Material.LEVER)
                                .name("&cPrevious Page &f(" + info.getCurrent() + "/" + info.getSize() + ")")
                                .lore("&7&oClick to return.")
                                .build())

                        .scheme(new MenuScheme(StandardSchemeMappings.STAINED_GLASS)
                                .mask("111111111")
                                .mask("100000001")
                                .mask("100000001")
                                .mask("100000001")
                                .mask("100101001")
                                .mask("111111111")
                                .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0)
                                .scheme(0, 0)
                                .scheme(0, 0)
                                .scheme(0, 0)
                                .scheme(0, 0, 0, 0)
                                .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0))

                        .itemSlots(new MenuScheme()
                                .mask("000000000")
                                .mask("011111110")
                                .mask("011111110")
                                .mask("011000110")
                                .mask("011111110")
                                .getMaskedIndexesImmutable())

                        .title("Island Chunk Loaders"));
    }
}
