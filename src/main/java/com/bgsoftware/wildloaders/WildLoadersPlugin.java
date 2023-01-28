package com.bgsoftware.wildloaders;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildloaders.api.WildLoaders;
import com.bgsoftware.wildloaders.api.WildLoadersAPI;
import com.bgsoftware.wildloaders.command.CommandsHandler;
import com.bgsoftware.wildloaders.gui.PaginatedChunkLoaderListGui;
import com.bgsoftware.wildloaders.handlers.DataHandler;
import com.bgsoftware.wildloaders.handlers.LoadersHandler;
import com.bgsoftware.wildloaders.handlers.NPCHandler;
import com.bgsoftware.wildloaders.handlers.ProvidersHandler;
import com.bgsoftware.wildloaders.handlers.SettingsHandler;
import com.bgsoftware.wildloaders.island.IslandChunkLoaderStorageDao;
import com.bgsoftware.wildloaders.island.impl.FileIslandChunkLoaderStorageDao;
import com.bgsoftware.wildloaders.listeners.BlocksListener;
import com.bgsoftware.wildloaders.listeners.ChunksListener;
import com.bgsoftware.wildloaders.listeners.PlayersListener;
import com.bgsoftware.wildloaders.metrics.Metrics;
import com.bgsoftware.wildloaders.nms.NMSAdapter;
import com.bgsoftware.wildloaders.utils.Pair;
import com.bgsoftware.wildloaders.utils.ServerVersion;
import com.bgsoftware.wildloaders.utils.database.Database;
import me.lucko.helper.Commands;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import javax.swing.text.html.Option;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class WildLoadersPlugin extends JavaPlugin implements WildLoaders {

    private static WildLoadersPlugin plugin;

    private SettingsHandler settingsHandler;
    private LoadersHandler loadersHandler;
    private NPCHandler npcHandler;
    private DataHandler dataHandler;
    private ProvidersHandler providersHandler;

    private NMSAdapter nmsAdapter;

    private boolean shouldEnable = true;

    private int chunkLoaderLimit;

    private IslandChunkLoaderStorageDao dao;

    @Override
    public void onLoad() {
        chunkLoaderLimit = getConfig().getInt("chunk-loader-limit", 5);
        plugin = this;
        new Metrics(this);

        shouldEnable = loadNMSAdapter();
        loadAPI();

        if (!shouldEnable)
            log("&cThere was an error while loading the plugin.");
    }

    @Override
    public void onEnable() {
        if (!shouldEnable) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        dataHandler = new DataHandler(this);
        loadersHandler = new LoadersHandler(this);
        npcHandler = new NPCHandler(this);
        providersHandler = new ProvidersHandler(this);
        settingsHandler = new SettingsHandler(this);

        this.dao = new FileIslandChunkLoaderStorageDao(this);
        dao.setup();

        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            getLogger().severe("Could not load WildLoaders. No economy service implementation is present.");
            return;
        }

        Economy economy = provider.getProvider();

        Commands.create()
                .assertPlayer()
                .handler(context -> {
                    Player sender = context.sender();
                    Optional<Island> optional = BentoBox.getInstance().getIslands().getIslandAt(sender.getLocation());
                    if (optional.isEmpty()) {
                        context.reply("&cThere is no island present at your location.");
                        return;
                    }

                    new PaginatedChunkLoaderListGui(optional.get(), dao, this, economy, sender).open();
                })
                .register("chunkloaders");

        getServer().getPluginManager().registerEvents(new BlocksListener(this, dao), this);
        getServer().getPluginManager().registerEvents(new ChunksListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayersListener(this), this);

        CommandsHandler commandsHandler = new CommandsHandler(this);
        getCommand("loader").setExecutor(commandsHandler);
        getCommand("loader").setTabCompleter(commandsHandler);

        Locale.reload();
    }

    @Override
    public void onDisable() {
        if (shouldEnable) {
            Database.stop();
            loadersHandler.removeChunkLoaders();
            npcHandler.killAllNPCs();
            dao.shutdown();
        }
    }

    private boolean loadNMSAdapter() {
        String version = null;

        if (ServerVersion.isLessThan(ServerVersion.v1_17)) {
            version = getServer().getClass().getPackage().getName().split("\\.")[3];
        } else {
            ReflectMethod<Integer> getDataVersion = new ReflectMethod<>(UnsafeValues.class, "getDataVersion");
            int dataVersion = getDataVersion.invoke(Bukkit.getUnsafe());

            List<Pair<Integer, String>> versions = Arrays.asList(
                    new Pair<>(2729, null),
                    new Pair<>(2730, "v117"),
                    new Pair<>(2865, "v1181"),
                    new Pair<>(2975, "v1182"),
                    new Pair<>(3105, "v119"),
                    new Pair<>(3117, "v1191"),
                    new Pair<>(3120, "v1192")
            );

            for (Pair<Integer, String> versionData : versions) {
                if (dataVersion <= versionData.getFirst()) {
                    version = versionData.getSecond();
                    break;
                }
            }

            if (version == null) {
                log("Data version: " + dataVersion);
            }
        }

        if (version != null) {
            try {
                nmsAdapter = (NMSAdapter) Class.forName(String.format("com.bgsoftware.wildloaders.nms.%s.NMSAdapter", version)).newInstance();
                return true;
            } catch (Exception error) {
                error.printStackTrace();
            }
        }

        log("&cThe plugin doesn't support your minecraft version.");
        log("&cPlease try a different version.");

        return false;
    }

    private void loadAPI() {
        try {
            Field instance = WildLoadersAPI.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, this);
        } catch (Exception ex) {
            log("Failed to set-up API - disabling plugin...");
            ex.printStackTrace();
            shouldEnable = false;
        }
    }

    public SettingsHandler getSettings() {
        return settingsHandler;
    }

    @Override
    public LoadersHandler getLoaders() {
        return loadersHandler;
    }

    @Override
    public NPCHandler getNPCs() {
        return npcHandler;
    }

    @Override
    public ProvidersHandler getProviders() {
        return providersHandler;
    }

    public NMSAdapter getNMSAdapter() {
        return nmsAdapter;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public static void log(String message) {
        plugin.getLogger().info(message);
    }

    public static WildLoadersPlugin getPlugin() {
        return plugin;
    }

    public int getChunkLoaderLimit() {
        return chunkLoaderLimit;
    }
}
