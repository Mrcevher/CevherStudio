package com.cevher.oto;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;

/** Oyuncu tercihleri oyuncunun PersistentDataContainer'ında saklanır; dosya tutulmaz. */
public final class PlayerData {

    private final CevherOto plugin;
    private final Map<Feature, NamespacedKey> keys = new EnumMap<>(Feature.class);

    public PlayerData(CevherOto plugin) {
        this.plugin = plugin;
        for (Feature f : Feature.values()) keys.put(f, new NamespacedKey(plugin, f.id));
    }

    /** Oyuncunun tercihi (yetki/dünya kontrolü yok). */
    public boolean isOn(Player p, Feature f) {
        Byte b = p.getPersistentDataContainer().get(keys.get(f), PersistentDataType.BYTE);
        return b == null ? plugin.settings().defaultOn(f) : b == 1;
    }

    public void set(Player p, Feature f, boolean on) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.set(keys.get(f), PersistentDataType.BYTE, (byte) (on ? 1 : 0));
    }

    public boolean toggle(Player p, Feature f) {
        boolean now = !isOn(p, f);
        set(p, f, now);
        return now;
    }

    /** Özellik şu an bu oyuncu için çalışmalı mı: yetki + tercih + dünya + oyun modu. */
    public boolean active(Player p, Feature f) {
        if (!p.hasPermission(f.permission()) || !isOn(p, f)) return false;
        if (plugin.settings().disabledWorlds.contains(p.getWorld().getName())) return false;
        GameMode gm = p.getGameMode();
        if (f == Feature.TOOLS) {
            return gm == GameMode.SURVIVAL || (gm == GameMode.ADVENTURE && plugin.settings().toolsAdventure);
        }
        return gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE;
    }
}
