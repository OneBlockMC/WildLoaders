package com.bgsoftware.wildloaders.gui;

import com.bgsoftware.wildloaders.api.loaders.ChunkLoader;
import com.bgsoftware.wildloaders.utils.Pair;
import com.google.common.collect.ImmutableMap;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.menu.Gui;
import me.lucko.helper.text3.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.Duration;

public class ChunkLoaderTimeGui extends Gui {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###");

    // Duration -> Pair<Friendly, Price>
    private static final ImmutableMap<Duration, Pair<String, Double>> DURATION_PRICE_MAP = ImmutableMap.<Duration, Pair<String, Double>>builder()
            .put(Duration.ofHours(1L), new Pair<>("&a1 Hour Time", 50_000D))
            .put(Duration.ofHours(2L), new Pair<>("&a2 Hours Time", 100_000D))
            .put(Duration.ofHours(3L), new Pair<>("&a3 Hours Time", 300_000D))
            .put(Duration.ofHours(12L), new Pair<>("&a4 Hours Time", 600_000D))
            .put(Duration.ofHours(16L), new Pair<>("&a16 Hours Time", 800_000D))
            .put(Duration.ofHours(20L), new Pair<>("&a20 Hours Time", 1_000_000D))
            .put(Duration.ofDays(1L), new Pair<>("&a1 Day Time", 1_200_000D))
            .put(Duration.ofDays(2L), new Pair<>("&a2 Days Time", 2_400_000D))
            .put(Duration.ofDays(3L), new Pair<>("&a3 Days Time", 3_600_000D))
            .build();

    private final ChunkLoader loader;
    private final Economy economy;

    public ChunkLoaderTimeGui(Player player, ChunkLoader loader, Economy economy) {
        super(player, 1, "&dBuy more time!");
        this.loader = loader;
        this.economy = economy;
    }

    @Override
    public void redraw() {
        DURATION_PRICE_MAP.forEach((duration, pair) -> {
            String friendly = pair.getFirst();
            double price = pair.getSecond();

            addItem(ItemStackBuilder.of(Material.PAPER)
                    .name(friendly)
                    .lore(
                            " ",
                            "&8* &fPrice: &2$" + DECIMAL_FORMAT.format(price),
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
                        } else {
                            loader.setTimeLeft(loader.getTimeLeft() + duration.toSeconds());
                        }

                        getPlayer().sendMessage(Text.colorize("&aYou have purchased " + friendly + " for your chunk loader."));
                    }));
        });
    }
}
