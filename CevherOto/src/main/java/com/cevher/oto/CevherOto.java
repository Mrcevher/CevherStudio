package com.cevher.oto;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CevherOto extends JavaPlugin {

    private Settings settings;
    private final Messages messages = new Messages();
    private PlayerData data;
    private Smelter smelter;
    private Compressor compressor;
    private Menu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new PlayerData(this);
        smelter = new Smelter(this);
        compressor = new Compressor(this);
        menu = new Menu(this);
        reloadAll();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PickupListener(this), this);
        pm.registerEvents(new ToolListener(this), this);
        pm.registerEvents(new RefillListener(this), this);
        pm.registerEvents(menu, this);

        OtoCommand cmd = new OtoCommand(this);
        for (String name : new String[]{"oto", "autopickup", "autosmelt", "autodrops", "besttools", "refill", "autoblock"}) {
            PluginCommand pc = getCommand(name);
            if (pc != null) {
                pc.setExecutor(cmd);
                pc.setTabCompleter(cmd);
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PapiHook(this).register();
            getLogger().info("PlaceholderAPI bağlantısı kuruldu.");
        }
    }

    /** config.yml'i ve fırın tariflerini yeniden yükler. */
    public void reloadAll() {
        reloadConfig();
        settings = new Settings(getConfig(), getLogger());
        messages.reload(getConfig());
        smelter.rebuild();
    }

    public Settings settings() { return settings; }
    public Messages messages() { return messages; }
    public PlayerData data() { return data; }
    public Smelter smelter() { return smelter; }
    public Compressor compressor() { return compressor; }
    public Menu menu() { return menu; }
}
