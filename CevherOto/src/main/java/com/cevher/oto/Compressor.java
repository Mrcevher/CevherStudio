package com.cevher.oto;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

/** Eski AutoBlock: külçe/maden -> blok. Sadece düz vanilya eşyalar. */
public final class Compressor {

    private final CevherOto plugin;
    private final Set<UUID> pending = new HashSet<>();
    private boolean scheduled;

    public Compressor(CevherOto plugin) {
        this.plugin = plugin;
    }

    public boolean isInput(Material m) {
        return plugin.settings().compressRules.containsKey(m);
    }

    /** Aynı tick'te gelen çok sayıda eşya için tek seferde, bir sonraki tick'te sıkıştırır. */
    public void schedule(Player p) {
        pending.add(p.getUniqueId());
        if (scheduled) return;
        scheduled = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            scheduled = false;
            List<UUID> list = new ArrayList<>(pending);
            pending.clear();
            for (UUID id : list) {
                Player pl = Bukkit.getPlayer(id);
                if (pl != null && plugin.data().active(pl, Feature.BLOCK)) compress(pl);
            }
        });
    }

    /** @return yapılan blok sayısı */
    public int compress(Player p) {
        PlayerInventory inv = p.getInventory();
        int made = 0;
        // Zincirleme dönüşümler için (külçe parçası -> külçe -> blok) birkaç tur.
        for (int pass = 0; pass < 3; pass++) {
            boolean changed = false;
            for (Map.Entry<Material, Settings.CompressRule> e : plugin.settings().compressRules.entrySet()) {
                ItemStack plain = new ItemStack(e.getKey());
                Settings.CompressRule rule = e.getValue();
                ItemStack[] st = inv.getStorageContents();
                int total = 0;
                for (ItemStack it : st) if (it != null && it.isSimilar(plain)) total += it.getAmount();
                int n = total / rule.amount();
                if (n == 0) continue;

                int remove = n * rule.amount();
                for (int i = 0; i < st.length && remove > 0; i++) {
                    ItemStack it = st[i];
                    if (it == null || !it.isSimilar(plain)) continue;
                    int take = Math.min(remove, it.getAmount());
                    remove -= take;
                    if (take == it.getAmount()) st[i] = null;
                    else it.setAmount(it.getAmount() - take);
                }
                inv.setStorageContents(st);

                int max = Math.max(1, rule.result().getMaxStackSize());
                for (int left = n; left > 0; ) {
                    int a = Math.min(left, max);
                    left -= a;
                    for (ItemStack over : inv.addItem(new ItemStack(rule.result(), a)).values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), over);
                    }
                }
                made += n;
                changed = true;
            }
            if (!changed) break;
        }
        return made;
    }
}
