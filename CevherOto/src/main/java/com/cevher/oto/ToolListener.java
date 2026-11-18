package com.cevher.oto;

import com.google.common.collect.Multimap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Eski BestTools. Hangi aletin en iyi olduğunu sabit listelerle değil, sunucunun kendi
 * kazma hızı hesabıyla (Block#getDestroySpeed, büyüler dahil) bulur. Böylece yeni sürüm
 * aletleri ve tool bileşeni olan özel aletler (Nexo/MMOItems) de doğru seçilir.
 */
public final class ToolListener implements Listener {

    private final CevherOto plugin;
    private final Map<UUID, Long> lastCombat = new HashMap<>();

    public ToolListener(CevherOto plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ blok

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeftClickBlock(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Player p = e.getPlayer();
        Settings s = plugin.settings();
        if (!plugin.data().active(p, Feature.TOOLS)) return;
        if (s.toolsSneakBypass && p.isSneaking()) return;
        if (inCombat(p)) return;
        Material type = block.getType();
        if (type.isAir() || s.toolBlockBlacklist.contains(type)) return;
        if (type.getHardness() <= 0f) return; // anında kırılan blok, eli değiştirmeye gerek yok

        PlayerInventory inv = p.getInventory();
        int held = inv.getHeldItemSlot();
        boolean hotbarOnly = plugin.data().isOn(p, Feature.HOTBAR);
        int last = hotbarOnly ? 8 : 35;

        double bestScore = blockScore(block, inv.getItem(held));
        int bestSlot = -1;
        for (int slot = 0; slot <= last; slot++) {
            if (slot == held) continue;
            ItemStack it = inv.getItem(slot);
            if (!usable(it)) continue;
            double score = blockScore(block, it);
            if (score > bestScore + 1e-6) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0) switchTo(p, bestSlot);
    }

    /** Doğru aletse (düşüş verir) büyük bonus + gerçek kazma hızı + büyü tercihleri. */
    private double blockScore(Block block, ItemStack it) {
        boolean empty = it == null || it.getType().isAir();
        ItemStack tool = empty ? ItemStack.empty() : it;
        double score = block.getDestroySpeed(tool, true);
        if (block.isPreferredTool(tool)) score += 1000;
        if (empty) return score;
        Material type = block.getType();
        int silk = it.getEnchantmentLevel(Enchantment.SILK_TOUCH);
        int fortune = it.getEnchantmentLevel(Enchantment.FORTUNE);
        if (isOre(type)) {
            if (silk > 0) score -= 500;     // madende Silk Touch istenmez
            score += fortune * 10;           // Fortune tercih edilir
        } else if (plugin.settings().silkPreferred.contains(type) && silk > 0) {
            score += 500;
        }
        return score;
    }

    private static boolean isOre(Material m) {
        String n = m.name();
        return n.endsWith("_ORE") || m == Material.ANCIENT_DEBRIS || m == Material.GILDED_BLACKSTONE
                || m == Material.AMETHYST_CLUSTER || m == Material.MELON || m == Material.GLOWSTONE;
    }

    // ------------------------------------------------------------------ savaş

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        // Oyuncu hasar alırsa savaşta say.
        if (e.getEntity() instanceof Player victim) lastCombat.put(victim.getUniqueId(), System.currentTimeMillis());

        Player p = null;
        if (e.getDamager() instanceof Player d) p = d;
        else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player d) {
            lastCombat.put(d.getUniqueId(), System.currentTimeMillis());
            return;
        }
        if (p == null) return;
        lastCombat.put(p.getUniqueId(), System.currentTimeMillis());

        Settings s = plugin.settings();
        if (!s.swordOnEnemies || !(e.getEntity() instanceof Enemy)) return;
        if (!plugin.data().active(p, Feature.TOOLS)) return;
        if (s.toolsSneakBypass && p.isSneaking()) return;

        PlayerInventory inv = p.getInventory();
        ItemStack hand = inv.getItemInMainHand();
        Material ht = hand.getType();
        // Menzilli/özel silah tutuyorsa oyuncunun seçimine saygı göster.
        if (ht == Material.BOW || ht == Material.CROSSBOW || ht == Material.TRIDENT || ht == Material.MACE) return;

        int held = inv.getHeldItemSlot();
        boolean hotbarOnly = plugin.data().isOn(p, Feature.HOTBAR);
        int last = hotbarOnly ? 8 : 35;
        double best = isWeapon(hand) ? damage(hand) : 0;
        int bestSlot = -1;
        for (int slot = 0; slot <= last; slot++) {
            if (slot == held) continue;
            ItemStack it = inv.getItem(slot);
            if (!usable(it) || !isWeapon(it)) continue;
            double d = damage(it);
            if (d > best + 1e-6) {
                best = d;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0) switchTo(p, bestSlot);
    }

    private boolean isWeapon(ItemStack it) {
        if (it == null || it.getType().isAir()) return false;
        Material m = it.getType();
        return Tag.ITEMS_SWORDS.isTagged(m) || (plugin.settings().axeAsWeapon && Tag.ITEMS_AXES.isTagged(m));
    }

    /** Eşyanın vuruş hasarı: özel attribute varsa onu, yoksa vanilya değerini kullanır. */
    @SuppressWarnings("deprecation")
    private static double damage(ItemStack it) {
        double dmg = 1;
        ItemMeta meta = it.getItemMeta();
        Multimap<Attribute, AttributeModifier> mods = (meta != null && meta.hasAttributeModifiers())
                ? meta.getAttributeModifiers(EquipmentSlot.HAND)
                : it.getType().getDefaultAttributeModifiers(EquipmentSlot.HAND);
        if (mods != null) {
            for (AttributeModifier mod : mods.get(Attribute.ATTACK_DAMAGE)) {
                if (mod.getOperation() == AttributeModifier.Operation.ADD_NUMBER) dmg += mod.getAmount();
            }
        }
        int sharp = it.getEnchantmentLevel(Enchantment.SHARPNESS);
        if (sharp > 0) dmg += 0.5 * sharp + 0.5;
        return dmg;
    }

    private boolean inCombat(Player p) {
        Long t = lastCombat.get(p.getUniqueId());
        return t != null && System.currentTimeMillis() - t < plugin.settings().combatMillis;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastCombat.remove(e.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ ortak

    /** Kara listede değil ve kırılmak üzere değil. */
    private boolean usable(ItemStack it) {
        if (it == null || it.getType().isAir()) return false;
        if (plugin.settings().toolItemBlacklist.contains(it.getType())) return false;
        if (it.getItemMeta() instanceof Damageable d && d.hasMaxDamage() && !d.isUnbreakable()) {
            int remaining = d.getMaxDamage() - d.getDamage();
            if (remaining <= plugin.settings().minDurability) return false;
        }
        return true;
    }

    /** Hotbar'daysa o slota geçer; değilse boş hotbar slotuna veya favori slota taşıyıp geçer. */
    private void switchTo(Player p, int slot) {
        PlayerInventory inv = p.getInventory();
        if (slot <= 8) {
            inv.setHeldItemSlot(slot);
            return;
        }
        int target = -1;
        for (int i = 0; i <= 8; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) {
                target = i;
                break;
            }
        }
        if (target < 0) target = plugin.settings().favoriteSlot;
        ItemStack moving = inv.getItem(slot);
        ItemStack displaced = inv.getItem(target);
        inv.setItem(target, moving);
        inv.setItem(slot, displaced);
        inv.setHeldItemSlot(target);
    }
}
