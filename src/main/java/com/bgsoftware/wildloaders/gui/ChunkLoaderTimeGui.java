package com.bgsoftware.wildloaders.gui;

import com.bgsoftware.wildloaders.WildLoadersPlugin;
import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.api.npc.ChunkLoaderNPC;
import com.bgsoftware.wildloaders.utils.Pair;
import com.google.common.collect.ImmutableMap;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Gui;
import me.lucko.helper.text3.Text;
import me.lucko.helper.time.DurationFormatter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.function.Function;

public class ChunkLoaderTimeGui extends Gui {

    private final WildLoadersPlugin plugin;

    private static final double PRICE_PER_SECOND = 13.3D;

    private static final Function<ChunkLoader, Duration> TIME_LEFT_ACCUMULATOR = loader -> Duration.ofDays(3L).minus(Duration.ofSeconds(loader.getTimeLeft()));

    // Duration -> Pair<Friendly, Price>
    private static final ImmutableMap<Duration, Pair<String, Double>> DURATION_PRICE_MAP = ImmutableMap.<Duration, Pair<String, Double>>builder()
            .put(Duration.ofHours(1L), new Pair<>("&a1 Hour Time", 50_000D))
            .put(Duration.ofHours(2L), new Pair<>("&a2 Hours Time", 100_000D))
            .put(Duration.ofHours(3L), new Pair<>("&a3 Hours Time", 300_000D))
            .put(Duration.ofHours(12L), new Pair<>("&a4 Hours Time", 600_000D))
            .put(Duration.ofHours(16L), new Pair<>("&a16 Hours Time", 800_000D))
            .put(Duration.ofHours(20L), new Pair<>("&a20 Hours Time", 1_000_000D))
            .put(Duration.ofDays(1L), new Pair<>("&a1 Day Time", 1_200_000D))
            .build();

    private final ChunkLoader loader;
    private final Economy economy;

    public ChunkLoaderTimeGui(Player player, ChunkLoader loader, Economy economy, WildLoadersPlugin plugin) {
        super(player, 1, "&dBuy more time!");
        this.loader = loader;
        this.economy = economy;
        this.plugin = plugin;
    }

    @Override
    public void redraw() {
        setItem(7, ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build(null));

        setItem(8, ItemStackBuilder.of(Material.GOLD_BLOCK)
                .name("&aBuy Max Time")
                .lore(
                        "&fBuying this will refill your",
                        "&fChunk Loader up to the max",
                        "&famount of time! &7(3 days)",
                        " ",
                        "&8* &fTime: &b" + DurationFormatter.format(TIME_LEFT_ACCUMULATOR.apply(loader), true),
                        "&8* &fPrice: &2$&f" + WildLoadersPlugin.DECIMAL_FORMAT.format(TIME_LEFT_ACCUMULATOR.apply(loader).toSeconds() * PRICE_PER_SECOND),
                        " "
                )
                .build(() -> {
                    close();
                    Duration timeToAdd = TIME_LEFT_ACCUMULATOR.apply(loader);

                    double balanceRequired = timeToAdd.toSeconds() * PRICE_PER_SECOND;
                    if (!economy.has(getPlayer(), balanceRequired)) {
                        getPlayer().sendMessage(Text.colorize("&cYou must have &c&l$" + WildLoadersPlugin.DECIMAL_FORMAT.format(balanceRequired) + " &cto purchase this!"));
                        return;
                    }

                    economy.withdrawPlayer(getPlayer(), balanceRequired);
                    loader.setTimeLeft(loader.getTimeLeft() + timeToAdd.toSeconds());
                    getPlayer().sendMessage(Text.colorize("&aYou have purchased the max amount of time for your Chunk Loader!"));
                }));

        DURATION_PRICE_MAP.forEach((duration, pair) -> {
            String friendly = pair.getFirst();
            double price = pair.getSecond();

            addItem(ItemStackBuilder.of(Material.PAPER)
                    .name(friendly)
                    .lore(
                            " ",
                            "&8* &fPrice: &2$" + WildLoadersPlugin.DECIMAL_FORMAT.format(price),
                            " "
                    )
                    .build(() -> {
                        if (duration.plusSeconds(loader.getTimeLeft()).toSeconds() > ChunkLoader.MAX_ACTIVITY_TIME.toSeconds()) {
                            close();
                            getPlayer().sendMessage(Text.colorize("&cYou cannot add this much time to your chunk loader. It will surpass the 3 day time limit."));
                            return;
                        }

                        if (!economy.has(getPlayer(), price)) {
                            close();
                            getPlayer().sendMessage(Text.colorize("&cYou do not have enough money to purchase this amount of time."));
                            return;
                        }

                        close();
                        economy.withdrawPlayer(getPlayer(), price);

                        if (loader.isWaiting()) {
                            loader.setWaiting(false);
                            loader.setTimeLeft(duration.toSeconds());
                            plugin.getNPCs().createNPC(loader.getLocation());
                        } else {
                            loader.setTimeLeft(loader.getTimeLeft() + duration.toSeconds());
                        }

                        getPlayer().sendMessage(Text.colorize("&aYou have purchased " + friendly + " for your chunk loader."));
                    }));
        });
    }
}
