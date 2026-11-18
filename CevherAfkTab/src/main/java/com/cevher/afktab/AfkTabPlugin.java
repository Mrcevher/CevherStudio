package com.cevher.afktab;

import net.ess3.api.IUser;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Essentials AFK durumuna göre oyuncuyu diğerlerinin tab listesinden gizler (Paper unlistPlayer). */
public final class AfkTabPlugin extends JavaPlugin implements Listener {
    private static final String BYPASS = "cevherafktab.gor";
    private final Set<UUID> afk = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (UUID id : afk) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) apply(p, false);
        }
        afk.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAfk(AfkStatusChangeEvent e) {
        IUser u = e.getAffected();
        Player p = u.getBase();
        if (p == null) return;
        boolean now = e.getValue();
        if (now) afk.add(p.getUniqueId()); else afk.remove(p.getUniqueId());
        apply(p, now);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player viewer = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!viewer.isOnline() || viewer.hasPermission(BYPASS)) return;
            for (UUID id : afk) {
                Player t = Bukkit.getPlayer(id);
                if (t != null && !t.equals(viewer)) viewer.unlistPlayer(t);
            }
        }, 20L);
    }

    private void apply(Player target, boolean hide) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            if (hide) {
                if (!viewer.hasPermission(BYPASS)) viewer.unlistPlayer(target);
            } else {
                viewer.listPlayer(target);
            }
        }
    }
}
