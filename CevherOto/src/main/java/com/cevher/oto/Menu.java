package com.cevher.oto;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

/** /oto menüsü: tüm özellikler tek ekranda. */
public final class Menu implements Listener {

    private static final int COMPRESS_SLOT = 22;

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final CevherOto plugin;

    public Menu(CevherOto plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        FileConfiguration c = plugin.getConfig();
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 27, Messages.parse(c.getString("menu.baslik", "CevherOto")));
        holder.inventory = inv;
        fill(p, inv);
        p.openInventory(inv);
    }

    private void fill(Player p, Inventory inv) {
        FileConfiguration c = plugin.getConfig();
        Material fillerType = Material.matchMaterial(c.getString("menu.dolgu", "GRAY_STAINED_GLASS_PANE"));
        if (fillerType == null || !fillerType.isItem()) fillerType = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack filler = new ItemStack(fillerType);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Messages.item(" "));
        fm.setHideTooltip(true);
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        for (Feature f : Feature.values()) inv.setItem(f.menuSlot, featureItem(p, f));

        if (p.hasPermission(Feature.BLOCK.permission())) {
            ItemStack btn = new ItemStack(Material.ANVIL);
            ItemMeta m = btn.getItemMeta();
            m.displayName(Messages.item(c.getString("menu.sikistir-buton.isim", "Sıkıştır")));
            m.lore(Messages.itemLines(c.getStringList("menu.sikistir-buton.aciklama")));
            btn.setItemMeta(m);
            inv.setItem(COMPRESS_SLOT, btn);
        }
    }

    private ItemStack featureItem(Player p, Feature f) {
        FileConfiguration c = plugin.getConfig();
        Messages msg = plugin.messages();
        boolean perm = p.hasPermission(f.permission());
        boolean on = plugin.data().isOn(p, f);

        ItemStack it = new ItemStack(perm ? f.icon : Material.BARRIER);
        ItemMeta m = it.getItemMeta();
        m.displayName(Messages.item((on && perm ? "&a&l" : "&c&l") + msg.featureName(f)));
        List<String> lore = new ArrayList<>(c.getStringList("ozellikler." + f.id + ".aciklama"));
        lore.add("");
        if (perm) {
            lore.add(c.getString("menu.durum-satiri", "&7Durum: {durum}").replace("{durum}", msg.state(on)));
            lore.add(c.getString("menu.tikla", "&eTıkla"));
        } else {
            lore.add(c.getString("menu.yetkisiz", "&cYetkin yok"));
        }
        m.lore(Messages.itemLines(lore));
        m.setEnchantmentGlintOverride(on && perm);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot = e.getRawSlot();

        if (slot == COMPRESS_SLOT && p.hasPermission(Feature.BLOCK.permission())) {
            int made = plugin.compressor().compress(p);
            if (made > 0) plugin.messages().send(p, "sikistirildi", "{adet}", String.valueOf(made));
            else plugin.messages().send(p, "sikistirilacak-yok");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.4f);
            return;
        }

        Feature f = Feature.byMenuSlot(slot);
        if (f == null || !p.hasPermission(f.permission())) return;
        boolean on = plugin.data().toggle(p, f);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, on ? 1.4f : 0.8f);
        e.getView().getTopInventory().setItem(slot, featureItem(p, f));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Holder) e.setCancelled(true);
    }
}
