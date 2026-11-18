package com.cevher.oto;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Sunucudaki fırın tariflerini bir kez okuyup hızlı eritme tablosu oluşturur. */
public final class Smelter {

    private record Result(Material type, int amount, float exp) {}

    private final CevherOto plugin;
    private final Map<Material, Result> recipes = new EnumMap<>(Material.class);

    public Smelter(CevherOto plugin) {
        this.plugin = plugin;
    }

    public void rebuild() {
        recipes.clear();
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r;
            try {
                r = it.next();
            } catch (Throwable t) {
                continue;
            }
            if (!(r instanceof FurnaceRecipe fr)) continue;
            ItemStack res = fr.getResult();
            // Sonucu özel veri taşıyan (ör. Nexo) tarifleri alma.
            if (res.hasItemMeta() && !res.isSimilar(new ItemStack(res.getType()))) continue;
            if (fr.getInputChoice() instanceof RecipeChoice.MaterialChoice mc) {
                for (Material m : mc.getChoices()) {
                    recipes.putIfAbsent(m, new Result(res.getType(), res.getAmount(), fr.getExperience()));
                }
            }
        }
    }

    /**
     * Eşyayı eritir; eritilemiyorsa aynı eşyayı döndürür.
     * @param fromMob yaratıktan düştüyse true (sadece et listesi kullanılır)
     * @param xp eritme XP'si buraya eklenir (tek elemanlı dizi)
     */
    public ItemStack smelt(ItemStack in, boolean fromMob, float[] xp) {
        if (in == null || in.getType().isAir()) return in;
        // Özel eşyalara dokunma.
        if (!in.isSimilar(new ItemStack(in.getType()))) return in;
        Settings s = plugin.settings();
        Material type = in.getType();
        boolean allowed;
        if (fromMob) allowed = s.smeltMobFood && s.smeltMobList.contains(type);
        else allowed = s.smeltAll ? !s.smeltBlacklist.contains(type) : s.smeltList.contains(type);
        if (!allowed) return in;
        Result r = recipes.get(type);
        if (r == null) return in;
        if (xp != null) xp[0] += r.exp * in.getAmount();
        return new ItemStack(r.type, in.getAmount() * r.amount);
    }

    /** Kesirli XP'yi fırındaki gibi olasılıkla yuvarlayıp verir (Mending dahil). */
    public static void giveXp(Player p, float xp) {
        if (xp <= 0) return;
        int whole = (int) xp;
        if (ThreadLocalRandom.current().nextFloat() < xp - whole) whole++;
        if (whole > 0) p.giveExp(whole, true);
    }
}
