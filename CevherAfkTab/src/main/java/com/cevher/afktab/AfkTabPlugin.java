package com.cevher.afktab;

import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AfkTabPlugin extends JavaPlugin implements Listener {

    private final Set<UUID> afkListesi = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // kapanirken herkesi geri goster
        for (UUID id : afkListesi) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) goster(p);
        }
        afkListesi.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afkDegisti(AfkStatusChangeEvent e) {
        Player p = e.getAffected().getBase();
        if (p == null) return;

        if (e.getValue()) {
            afkListesi.add(p.getUniqueId());
            gizle(p);
        } else {
            afkListesi.remove(p.getUniqueId());
            goster(p);
        }
    }

    @EventHandler
    public void girdi(PlayerJoinEvent e) {
        Player yeni = e.getPlayer();
        if (yeni.hasPermission("cevherafktab.gor")) return;

        // tab paketleri gelmeden unlist etmek ise yaramiyor, biraz bekle
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (UUID id : afkListesi) {
                Player afk = Bukkit.getPlayer(id);
                if (afk != null && afk != yeni) yeni.unlistPlayer(afk);
            }
        }, 20L);
    }

    @EventHandler
    public void cikti(PlayerQuitEvent e) {
        afkListesi.remove(e.getPlayer().getUniqueId());
    }

    private void gizle(Player afk) {
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o == afk || o.hasPermission("cevherafktab.gor")) continue;
            o.unlistPlayer(afk);
        }
    }

    private void goster(Player afk) {
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o != afk) o.listPlayer(afk);
        }
    }
}
