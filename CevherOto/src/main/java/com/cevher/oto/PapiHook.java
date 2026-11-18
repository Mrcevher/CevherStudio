package com.cevher.oto;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/** %cevheroto_toplama% vb. -> Açık / Kapalı */
public final class PapiHook extends PlaceholderExpansion {

    private final CevherOto plugin;

    public PapiHook(CevherOto plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "cevheroto";
    }

    @Override
    public String getAuthor() {
        return "Cevher";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (p == null) return "";
        Feature f = Feature.byId(params);
        if (f == null) return null;
        boolean on = p.hasPermission(f.permission()) && plugin.data().isOn(p, f);
        return Messages.toSection(plugin.messages().state(on));
    }
}
