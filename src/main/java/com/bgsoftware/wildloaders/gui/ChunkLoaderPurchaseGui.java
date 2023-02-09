package com.bgsoftware.wildloaders.gui;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.loaders.LoaderData;
import com.bgsoftware.wildloaders.api.ChunkLoaderMetaDao;
import com.bgsoftware.wildloaders.utils.Pair;
import com.google.common.collect.HashBasedTable;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Gui;
import me.lucko.helper.menu.scheme.MenuScheme;
import me.lucko.helper.menu.scheme.StandardSchemeMappings;
import me.lucko.helper.text3.Text;
import me.lucko.helper.time.DurationFormatter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.database.objects.Island;

import java.time.Duration;
import java.util.Optional;

public class ChunkLoaderPurchaseGui extends Gui {

    private static final String NORMAL_LOADER_KEY = "normal_loader";

    private static final HashBasedTable<Integer, String, Pair<Duration, Double>> LOADER_INFO_TABLE = HashBasedTable.create();

    static {
        LOADER_INFO_TABLE.put(10, "&3&l5x5 &b&lChunk Loader", new Pair<>(Duration.ofHours(6L), 600_000D));
        LOADER_INFO_TABLE.put(12, "&3&l5x5 &b&lChunk Loader", new Pair<>(Duration.ofHours(12L), 900_000D));
        LOADER_INFO_TABLE.put(14, "&3&l5x5 &b&lChunk Loader", new Pair<>(Duration.ofDays(1L), 1_500_000D));
        LOADER_INFO_TABLE.put(16, "&3&l5x5 &b&lChunk Loader", new Pair<>(Duration.ofDays(3L), 3_900_000D));
    }

    private static final MenuScheme PANE_SCHEME = new MenuScheme(StandardSchemeMappings.STAINED_GLASS)
            .mask("111111111")
            .mask("101010101")
            .mask("111111111")
            .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0)
            .scheme(0, 0, 0, 0, 0, 0)
            .scheme(0, 0, 0, 0, 0, 0, 0, 0, 0);

    private final WildLoadersPlugin plugin;
    private final Economy economy;
    private final Island island;
    private final ChunkLoaderMetaDao dao;

    public ChunkLoaderPurchaseGui(Player player, Island island, WildLoadersPlugin plugin, Economy economy, ChunkLoaderMetaDao dao) {
        super(player, 3, "&dBuy Chunk Loaders");
        this.plugin = plugin;
        this.economy = economy;
        this.island = island;
        this.dao = dao;
    }

    @Override
    public void redraw() {
        setFallbackGui(player -> new PaginatedChunkLoaderListGui(island, dao, plugin, economy, player));

        PANE_SCHEME.apply(this);

        LOADER_INFO_TABLE.cellSet().forEach(cell -> {
            if (cell.getRowKey() == null || cell.getColumnKey() == null || cell.getValue() == null) return;

            int slot = cell.getRowKey();
            String display = cell.getColumnKey();
            Pair<Duration, Double> pair = cell.getValue();

            Duration duration = pair.getFirst();
            double price = pair.getSecond();

            setItem(slot, ItemStackBuilder.of(Material.BEACON)
                    .name(display)
                    .lore(
                            " ",
                            "&8&l* &fPreloaded time: &a" + DurationFormatter.format(duration, true),
                            "&8&l* &fPrice: &a$" + WildLoadersPlugin.DECIMAL_FORMAT.format(price),
                            " ",
                            "&cYou can have a max of " + plugin.getChunkLoaderLimit(),
                            "&cChunk Loaders per island!",
                            " ",
                            "&6&lClick &fto buy!"
                    )
                    .build(() -> {
                        close();

                        Optional<LoaderData> optional = plugin.getLoaders().getLoaderData(NORMAL_LOADER_KEY);
                        if (optional.isEmpty()) {
                            getPlayer().sendMessage(Text.colorize("&cThere was an error purchasing this chunk loader. Please contact an admin for help"));
                            return;
                        }

                        if (!economy.has(getPlayer(), price)) {
                            getPlayer().sendMessage(Text.colorize("&cYou do not have enough money to purchase this chunk loader."));
                            return;
                        }

                        if (getPlayer().getInventory().firstEmpty() == -1) {
                            getPlayer().sendMessage(Text.colorize("&cPlease make space in your inventory before purchasing a chunk loader."));
                            return;
                        }

                        LoaderData data = optional.get();
                        ItemStack stack = data.getLoaderItem(duration.toSeconds());
                        getPlayer().getInventory().addItem(stack);
                        economy.withdrawPlayer(getPlayer(), price);

                        getPlayer().sendMessage(Text.colorize("&aYour chunk loader purchase was successful."));
                    }));
        });
    }
}
