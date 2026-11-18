package com.cevher.oto;

import org.bukkit.Material;

/** Oyuncunun açıp kapatabildiği özellikler. */
public enum Feature {
    PICKUP("toplama", Material.HOPPER, 10),
    SMELT("eritme", Material.BLAST_FURNACE, 11),
    DROPS("yaratik", Material.BONE, 12),
    BLOCK("blok", Material.IRON_BLOCK, 13),
    TOOLS("alet", Material.DIAMOND_PICKAXE, 14),
    HOTBAR("hotbar", Material.ITEM_FRAME, 15),
    REFILL("doldurma", Material.CHEST, 16);

    public final String id;
    public final Material icon;
    public final int menuSlot;

    Feature(String id, Material icon, int menuSlot) {
        this.id = id;
        this.icon = icon;
        this.menuSlot = menuSlot;
    }

    /** HOTBAR, En İyi Alet'in alt ayarı olduğu için onun yetkisini kullanır. */
    public String permission() {
        return "cevheroto." + (this == HOTBAR ? TOOLS.id : id);
    }

    public static Feature byId(String id) {
        if (id == null) return null;
        for (Feature f : values()) if (f.id.equalsIgnoreCase(id)) return f;
        return null;
    }

    public static Feature byMenuSlot(int slot) {
        for (Feature f : values()) if (f.menuSlot == slot) return f;
        return null;
    }
}
