package com.cevher.oto;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Eski AutoPickup (+ AutoBlock tetikleyicileri).
 * <ul>
 *   <li>Bloğun kendi düşüşleri: BlockDropItemEvent (Fortune/Silk dahil, eritme burada)</li>
 *   <li>Bloğa bağlı düşüşler (şeker kamışı, meşale, sandık içi, Nexo blokları vb.):
 *       kırılan konumu birkaç tick izleyip ItemSpawnEvent ile yakalar</li>
 *   <li>Yaratık düşüşleri: EntityDeathEvent (+ MythicMobs gibi sonradan düşenler için izleme)</li>
 * </ul>
 */
public final class PickupListener implements Listener {

    private record Pos(UUID world, int x, int y, int z) {
        static Pos of(Block b) {
            return new Pos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
        }
        static Pos of(Location l) {
            return new Pos(l.getWorld().getUID(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
        }
    }

    private record Track(UUID player, int expireTick, Feature feature) {}

    /** Üst üste büyüyen bitkiler: kırılınca üstündekiler de düşer. */
    private static final Set<Material> GROWS_UP = EnumSet.of(
            Material.SUGAR_CANE, Material.BAMBOO, Material.CACTUS, Material.KELP, Material.KELP_PLANT,
            Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT, Material.BIG_DRIPLEAF,
            Material.BIG_DRIPLEAF_STEM, Material.POINTED_DRIPSTONE, Material.CHORUS_PLANT, Material.CHORUS_FLOWER);
    /** Aşağı sarkanlar. */
    private static final Set<Material> GROWS_DOWN = EnumSet.of(
            Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT, Material.CAVE_VINES, Material.CAVE_VINES_PLANT,
            Material.POINTED_DRIPSTONE, Material.VINE);
    private static final BlockFace[] SIDES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
    private static final int TRACK_TICKS = 3;

    private final CevherOto plugin;
    private final Map<Pos, Track> tracked = new HashMap<>();
    private final Set<UUID> handledItems = new HashSet<>();
    private final Map<UUID, Long> lastFullWarn = new HashMap<>();

    public PickupListener(CevherOto plugin) {
        this.plugin = plugin;
        // Süresi dolan izleri temizle.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int now = Bukkit.getCurrentTick();
            tracked.values().removeIf(t -> t.expireTick < now);
            handledItems.clear();
        }, 20L, 20L);
    }

    // ------------------------------------------------------------------ bloklar

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (!plugin.data().active(p, Feature.PICKUP)) return;
        Settings s = plugin.settings();
        if (s.pickupBlacklist.contains(b.getType())) return;

        // XP direkt oyuncuya (Mending çalışır).
        if (s.pickupXpDirect && e.getExpToDrop() > 0
                && !(b.getType() == Material.SPAWNER && s.pickupSkipSpawnerXp)) {
            p.giveExp(e.getExpToDrop(), true);
            e.setExpToDrop(0);
        }

        if (!e.isDropItems()) return;
        int expire = Bukkit.getCurrentTick() + TRACK_TICKS;
        Track t = new Track(p.getUniqueId(), expire, Feature.PICKUP);
        tracked.put(Pos.of(b), t);
        for (BlockFace f : SIDES) tracked.put(Pos.of(b.getRelative(f)), t);
        if (GROWS_UP.contains(b.getType())) column(b, BlockFace.UP, t);
        if (GROWS_DOWN.contains(b.getType())) column(b, BlockFace.DOWN, t);
    }

    private void column(Block start, BlockFace dir, Track t) {
        Block cur = start;
        for (int i = 0; i < 64; i++) {
            cur = cur.getRelative(dir);
            Material m = cur.getType();
            if (!GROWS_UP.contains(m) && !GROWS_DOWN.contains(m)) {
                tracked.put(Pos.of(cur), t); // en uçtaki bağlı blok (ör. çiçek) için
                break;
            }
            tracked.put(Pos.of(cur), t);
            for (BlockFace f : SIDES) tracked.put(Pos.of(cur.getRelative(f)), t);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        if (!plugin.data().active(p, Feature.PICKUP)) return;
        Settings s = plugin.settings();
        if (s.pickupBlacklist.contains(e.getBlockState().getType())) return;

        boolean smelt = plugin.data().active(p, Feature.SMELT);
        float[] xp = {0f};
        Location dropAt = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        Iterator<Item> it = e.getItems().iterator();
        while (it.hasNext()) {
            Item item = it.next();
            ItemStack stack = item.getItemStack();
            if (smelt) stack = plugin.smelter().smelt(stack, false, xp);
            List<ItemStack> left = give(p, stack);
            if (left.isEmpty() || s.voidOnFull) {
                it.remove();
            } else {
                // Sığmayan kısım yerde kalsın (tek eşya olarak; fazlası ayrı düşer).
                item.setItemStack(left.get(0));
                handledItems.add(item.getUniqueId());
                for (int i = 1; i < left.size(); i++) dropNaturally(dropAt, left.get(i));
            }
        }
        if (s.smeltXp && xp[0] > 0) Smelter.giveXp(p, xp[0]);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        if (tracked.isEmpty() || dropping) return;
        Item item = e.getEntity();
        if (item.getThrower() != null || handledItems.contains(item.getUniqueId())) return;
        Track t = tracked.get(Pos.of(item.getLocation()));
        if (t == null || t.expireTick < Bukkit.getCurrentTick()) return;
        Player p = Bukkit.getPlayer(t.player);
        if (p == null || !p.getWorld().equals(item.getWorld()) || !plugin.data().active(p, t.feature)) return;
        if (t.feature == Feature.PICKUP && plugin.settings().pickupBlacklist.contains(item.getItemStack().getType())) return;

        List<ItemStack> left = give(p, item.getItemStack());
        if (left.isEmpty()) {
            e.setCancelled(true);
        } else {
            item.setItemStack(left.get(0));
            handledItems.add(item.getUniqueId());
            for (int i = 1; i < left.size(); i++) dropNaturally(item.getLocation(), left.get(i));
        }
    }

    // ------------------------------------------------------------------ yaratıklar

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity ent = e.getEntity();
        Player p = ent.getKiller();
        if (p == null || ent == p || !plugin.data().active(p, Feature.DROPS)) return;
        Settings s = plugin.settings();
        if (s.entityBlacklist.contains(ent.getType())) return;
        if (!p.getWorld().equals(ent.getWorld())) return;

        boolean smelt = plugin.data().active(p, Feature.SMELT);
        float[] xp = {0f};
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : e.getDrops()) {
            if (stack == null || stack.getType().isAir()) continue;
            if (smelt) stack = plugin.smelter().smelt(stack, true, xp);
            remaining.addAll(give(p, stack));
        }
        e.getDrops().clear();
        e.getDrops().addAll(remaining);
        if (s.smeltXp && xp[0] > 0) Smelter.giveXp(p, xp[0]);

        if (s.dropsXpDirect && e.getDroppedExp() > 0) {
            p.giveExp(e.getDroppedExp(), true);
            e.setDroppedExp(0);
        }

        // MythicMobs vb. ölümden hemen sonra kendi düşüşlerini oluşturan eklentiler için.
        Track t = new Track(p.getUniqueId(), Bukkit.getCurrentTick() + TRACK_TICKS, Feature.DROPS);
        Block at = ent.getLocation().getBlock();
        tracked.putIfAbsent(Pos.of(at), t);
        tracked.putIfAbsent(Pos.of(at.getRelative(BlockFace.UP)), t);
    }

    // ------------------------------------------------------------------ AutoBlock tetikleyicileri

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!plugin.settings().compressOnPickup) return;
        if (!plugin.compressor().isInput(e.getItem().getItemStack().getType())) return;
        if (plugin.data().active(p, Feature.BLOCK)) plugin.compressor().schedule(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!plugin.settings().compressOnClose) return;
        InventoryType type = e.getInventory().getType();
        if (type == InventoryType.CRAFTING || type == InventoryType.CREATIVE) return;
        if (plugin.data().active(p, Feature.BLOCK)) plugin.compressor().schedule(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastFullWarn.remove(e.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ yardımcılar

    /**
     * Eşyayı oyuncuya verir. Sığmazsa ve Otomatik Blok açıksa önce envanteri sıkıştırıp tekrar dener.
     * @return sığmayan eşyalar (boşsa hepsi verildi)
     */
    private List<ItemStack> give(Player p, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) return List.of();
        Map<Integer, ItemStack> left = p.getInventory().addItem(stack.clone());
        boolean block = plugin.data().active(p, Feature.BLOCK);
        if (!left.isEmpty() && block && plugin.compressor().compress(p) > 0) {
            left = p.getInventory().addItem(left.values().toArray(new ItemStack[0]));
        }
        if (block && plugin.compressor().isInput(stack.getType())) plugin.compressor().schedule(p);
        if (left.isEmpty()) return List.of();
        warnFull(p);
        return new ArrayList<>(left.values());
    }

    private void warnFull(Player p) {
        Settings s = plugin.settings();
        long now = System.currentTimeMillis();
        Long last = lastFullWarn.get(p.getUniqueId());
        if (last != null && now - last < s.fullCooldownMs) return;
        lastFullWarn.put(p.getUniqueId(), now);
        if (s.fullMessage) plugin.messages().send(p, "envanter-dolu");
        if (s.fullTitle) {
            p.showTitle(net.kyori.adventure.title.Title.title(
                    Messages.parse(s.fullTitle1), Messages.parse(s.fullTitle2)));
        }
    }

    /** Kendi bıraktığımız eşyaları tekrar yakalamamak için (sonsuz döngü koruması). */
    private boolean dropping;

    private void dropNaturally(Location at, ItemStack stack) {
        dropping = true;
        try {
            at.getWorld().dropItemNaturally(at, stack);
        } finally {
            dropping = false;
        }
    }
}
