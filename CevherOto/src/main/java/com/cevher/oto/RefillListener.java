package com.cevher.oto;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;

import java.util.EnumSet;
import java.util.Set;

// Eldeki yığın bitince (blok koyma, yeme, fırlatma) ya da alet kırılınca envanterdeki aynısını o slota getirir.
public final class RefillListener implements Listener {

    /** Yiyip içtikten sonra elde kalan boş kaplar. */
    private static final Set<Material> LEFTOVER_CONTAINERS = EnumSet.of(
            Material.BOWL, Material.GLASS_BOTTLE, Material.BUCKET);

    private final CevherOto plugin;

    public RefillListener(CevherOto plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        watch(e.getPlayer(), e.getHand(), e.getItemInHand(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        watch(e.getPlayer(), e.getHand(), e.getItem(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThrow(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player p)) return;
        PlayerInventory inv = p.getInventory();
        // Kartopu, yumurta, ender incisi, iksir vb. - hangi elde olduğunu bul.
        ItemStack main = inv.getItemInMainHand();
        ItemStack off = inv.getItemInOffHand();
        // Fırlatılan eşya hangi eldeyse onu izle (ana el öncelikli, vanilya ile aynı).
        if (THROWABLES.contains(main.getType())) watch(p, EquipmentSlot.HAND, main, false);
        else if (THROWABLES.contains(off.getType())) watch(p, EquipmentSlot.OFF_HAND, off, false);
    }

    /** Elden tüketilerek fırlatılan eşyalar (mızrak sadakatle geri döndüğü için dahil değil). */
    private static final Set<Material> THROWABLES = EnumSet.of(
            Material.SNOWBALL, Material.EGG, Material.BLUE_EGG, Material.BROWN_EGG, Material.ENDER_PEARL,
            Material.SPLASH_POTION, Material.LINGERING_POTION, Material.EXPERIENCE_BOTTLE, Material.WIND_CHARGE);

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(PlayerItemBreakEvent e) {
        if (!plugin.settings().refillBrokenTools) return;
        Player p = e.getPlayer();
        ItemStack broken = e.getBrokenItem();
        Material type = broken.getType();
        // Sadece elde tutulan aletler (zırh/elytra kırılınca el doldurulmaz).
        EquipmentSlot natural = type.getEquipmentSlot();
        if (natural != EquipmentSlot.HAND && natural != EquipmentSlot.OFF_HAND) return;
        PlayerInventory inv = p.getInventory();
        boolean inMain = inv.getItemInMainHand().getType() == type;
        boolean inOff = inv.getItemInOffHand().getType() == type;
        EquipmentSlot hand = (!inMain && inOff) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        watch(p, hand, broken, true);
    }

    private void watch(Player p, EquipmentSlot hand, ItemStack used, boolean tool) {
        if (used == null || used.getType().isAir()) return;
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;
        if (!plugin.data().active(p, Feature.REFILL)) return;
        // Yığında hâlâ eşya kalacaksa gerek yok.
        if (!tool && used.getAmount() > 1) return;

        ItemStack template = used.clone();
        template.setAmount(1);
        int slot = hand == EquipmentSlot.HAND ? p.getInventory().getHeldItemSlot() : 40;
        Bukkit.getScheduler().runTask(plugin, () -> refill(p, slot, template, tool));
    }

    private void refill(Player p, int slot, ItemStack template, boolean tool) {
        if (!p.isOnline()) return;
        PlayerInventory inv = p.getInventory();
        ItemStack now = inv.getItem(slot);
        if (now != null && !now.getType().isAir()) {
            // Çorba/iksir sonrası kalan kabı kenara koy, yoksa doldurma.
            if (!LEFTOVER_CONTAINERS.contains(now.getType()) || !moveAway(inv, slot)) return;
        }
        int found = find(inv, slot, template, tool);
        if (found < 0) return;
        inv.setItem(slot, inv.getItem(found));
        inv.setItem(found, null);
    }

    /** Önce ana envanter (9-35), sonra hotbar. Alette aynı türün en az hasarlısı seçilir. */
    private static int find(PlayerInventory inv, int exclude, ItemStack template, boolean tool) {
        int best = -1;
        int bestDamage = Integer.MAX_VALUE;
        for (int pass = 0; pass < 2; pass++) {
            int from = pass == 0 ? 9 : 0, to = pass == 0 ? 35 : 8;
            for (int i = from; i <= to; i++) {
                if (i == exclude) continue;
                ItemStack it = inv.getItem(i);
                if (it == null || it.getType().isAir()) continue;
                if (!tool) {
                    if (it.isSimilar(template)) return i;
                    continue;
                }
                if (it.getType() != template.getType()) continue;
                int dmg = it.getItemMeta() instanceof Damageable d ? d.getDamage() : 0;
                if (dmg < bestDamage) {
                    bestDamage = dmg;
                    best = i;
                }
            }
            if (best >= 0) return best;
        }
        return best;
    }

    private static boolean moveAway(PlayerInventory inv, int slot) {
        ItemStack container = inv.getItem(slot);
        for (int i = 9; i <= 35; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) {
                inv.setItem(i, container);
                inv.setItem(slot, null);
                return true;
            }
            if (it.isSimilar(container) && it.getAmount() + container.getAmount() <= it.getMaxStackSize()) {
                it.setAmount(it.getAmount() + container.getAmount());
                inv.setItem(slot, null);
                return true;
            }
        }
        return false;
    }
}
