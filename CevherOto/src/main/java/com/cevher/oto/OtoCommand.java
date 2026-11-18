package com.cevher.oto;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// /oto ve eski eklentilerden alışık olunan kısayollar (/auto, /autosmelt, /drops, /bt, /refill, /blok)
public final class OtoCommand implements TabExecutor {

    private final CevherOto plugin;

    public OtoCommand(CevherOto plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        String arg = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";

        switch (name) {
            case "autopickup" -> {
                if (arg.equals("reload")) return reload(sender);
                return toggle(sender, Feature.PICKUP);
            }
            case "autosmelt" -> { return toggle(sender, Feature.SMELT); }
            case "autodrops" -> { return toggle(sender, Feature.DROPS); }
            case "refill" -> { return toggle(sender, Feature.REFILL); }
            case "besttools" -> {
                if (arg.equals("hotbar")) return toggle(sender, Feature.HOTBAR);
                if (arg.equals("reload")) return reload(sender);
                return toggle(sender, Feature.TOOLS);
            }
            case "autoblock" -> {
                if (arg.equals("oto") || arg.equals("toggle")) return toggle(sender, Feature.BLOCK);
                if (arg.equals("reload")) return reload(sender);
                return compress(sender);
            }
            default -> { /* oto */ }
        }

        if (arg.isEmpty()) {
            Player p = player(sender);
            if (p != null) plugin.menu().open(p);
            return true;
        }
        switch (arg) {
            case "yenile", "reload" -> { return reload(sender); }
            case "sikistir" -> { return compress(sender); }
            default -> {
                Feature f = Feature.byId(arg);
                if (f == null) {
                    plugin.messages().send(sender, "bilinmeyen");
                    return true;
                }
                return toggle(sender, f);
            }
        }
    }

    private boolean toggle(CommandSender sender, Feature f) {
        Player p = player(sender);
        if (p == null) return true;
        if (!p.hasPermission(f.permission())) {
            plugin.messages().send(p, "yetki-yok");
            return true;
        }
        boolean on = plugin.data().toggle(p, f);
        plugin.messages().send(p, on ? "acildi" : "kapandi", "{ozellik}", plugin.messages().featureName(f));
        return true;
    }

    private boolean compress(CommandSender sender) {
        Player p = player(sender);
        if (p == null) return true;
        if (!p.hasPermission(Feature.BLOCK.permission())) {
            plugin.messages().send(p, "yetki-yok");
            return true;
        }
        int made = plugin.compressor().compress(p);
        if (made > 0) plugin.messages().send(p, "sikistirildi", "{adet}", String.valueOf(made));
        else plugin.messages().send(p, "sikistirilacak-yok");
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("cevheroto.admin")) {
            plugin.messages().send(sender, "yetki-yok");
            return true;
        }
        plugin.reloadAll();
        plugin.messages().send(sender, "yenilendi");
        return true;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player p) return p;
        plugin.messages().send(sender, "sadece-oyuncu");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) return List.of();
        List<String> options = new ArrayList<>();
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "oto" -> {
                for (Feature f : Feature.values()) if (sender.hasPermission(f.permission())) options.add(f.id);
                if (sender.hasPermission(Feature.BLOCK.permission())) options.add("sikistir");
                if (sender.hasPermission("cevheroto.admin")) options.add("yenile");
            }
            case "besttools" -> options.add("hotbar");
            case "autoblock" -> options.add("oto");
            default -> { }
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        options.removeIf(o -> !o.startsWith(prefix));
        return options;
    }
}
