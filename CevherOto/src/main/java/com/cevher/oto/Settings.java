package com.cevher.oto;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.logging.Logger;

/** config.yml'in önbelleğe alınmış hali. Her /oto yenile'de yeniden oluşturulur. */
public final class Settings {

    public record CompressRule(int amount, Material result) {}

    public final Set<String> disabledWorlds = new HashSet<>();
    private final Map<Feature, Boolean> defaults = new EnumMap<>(Feature.class);

    // toplama
    public final boolean pickupXpDirect, pickupSkipSpawnerXp, voidOnFull;
    public final Set<Material> pickupBlacklist;
    // yaratik
    public final boolean dropsXpDirect;
    public final Set<EntityType> entityBlacklist = EnumSet.noneOf(EntityType.class);
    // envanter dolu
    public final boolean fullMessage, fullTitle;
    public final long fullCooldownMs;
    public final String fullTitle1, fullTitle2;
    // eritme
    public final boolean smeltAll, smeltMobFood, smeltXp;
    public final Set<Material> smeltList, smeltBlacklist, smeltMobList;
    // blok
    public final boolean compressOnPickup, compressOnClose;
    public final Map<Material, CompressRule> compressRules = new LinkedHashMap<>();
    // alet
    public final int favoriteSlot, combatMillis, minDurability;
    public final boolean toolsAdventure, toolsSneakBypass, swordOnEnemies, axeAsWeapon;
    public final Set<Material> toolBlockBlacklist, toolItemBlacklist, silkPreferred;
    // doldurma
    public final boolean refillBrokenTools;

    public Settings(FileConfiguration c, Logger log) {
        disabledWorlds.addAll(c.getStringList("devre-disi-dunyalar"));
        for (Feature f : Feature.values()) {
            defaults.put(f, c.getBoolean("varsayilan." + f.id, f == Feature.PICKUP || f == Feature.DROPS || f == Feature.HOTBAR));
        }

        pickupXpDirect = c.getBoolean("toplama.xp-direkt", true);
        pickupSkipSpawnerXp = c.getBoolean("toplama.spawner-xp-verme", true);
        voidOnFull = c.getBoolean("toplama.doluysa-yok-et", false);
        pickupBlacklist = materials(c, "toplama.kara-liste", log);

        dropsXpDirect = c.getBoolean("yaratik.xp-direkt", true);
        for (String s : c.getStringList("yaratik.kara-liste")) {
            try {
                entityBlacklist.add(EntityType.valueOf(s.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                log.warning("Geçersiz yaratık türü (yaratik.kara-liste): " + s);
            }
        }

        fullMessage = c.getBoolean("envanter-dolu.mesaj", true);
        fullCooldownMs = Math.max(0, c.getLong("envanter-dolu.bekleme-saniye", 15)) * 1000L;
        fullTitle = c.getBoolean("envanter-dolu.baslik.aktif", true);
        fullTitle1 = c.getString("envanter-dolu.baslik.satir1", "");
        fullTitle2 = c.getString("envanter-dolu.baslik.satir2", "");

        smeltAll = c.getBoolean("eritme.tum-tarifler", false);
        smeltList = materials(c, "eritme.liste", log);
        smeltBlacklist = materials(c, "eritme.kara-liste", log);
        smeltMobFood = c.getBoolean("eritme.yaratik-etleri", true);
        smeltMobList = materials(c, "eritme.yaratik-listesi", log);
        smeltXp = c.getBoolean("eritme.xp-ver", true);

        compressOnPickup = c.getBoolean("blok.yerden-alinca", true);
        compressOnClose = c.getBoolean("blok.envanter-kapaninca", true);
        ConfigurationSection rules = c.getConfigurationSection("blok.tarifler");
        if (rules != null) {
            for (String key : rules.getKeys(false)) {
                Material in = Material.matchMaterial(key);
                Material out = Material.matchMaterial(rules.getString(key + ".sonuc", ""));
                int amount = rules.getInt(key + ".adet", 9);
                if (in == null || out == null || !in.isItem() || !out.isItem() || amount < 2) {
                    log.warning("Geçersiz blok tarifi atlandı: " + key);
                    continue;
                }
                compressRules.put(in, new CompressRule(amount, out));
            }
        }

        favoriteSlot = Math.min(9, Math.max(1, c.getInt("alet.favori-slot", 9))) - 1;
        toolsAdventure = c.getBoolean("alet.macera-modunda", false);
        toolsSneakBypass = c.getBoolean("alet.egilince-devre-disi", true);
        combatMillis = Math.max(0, c.getInt("alet.savas-bekleme-saniye", 3)) * 1000;
        minDurability = Math.max(0, c.getInt("alet.min-dayaniklilik", 5));
        swordOnEnemies = c.getBoolean("alet.dusmana-kilic", true);
        axeAsWeapon = c.getBoolean("alet.balta-silah", false);
        toolBlockBlacklist = materials(c, "alet.blok-kara-liste", log);
        toolItemBlacklist = materials(c, "alet.esya-kara-liste", log);
        silkPreferred = materials(c, "alet.silk-tercih", log);

        refillBrokenTools = c.getBoolean("doldurma.kirilan-aleti-degistir", true);
    }

    public boolean defaultOn(Feature f) {
        return defaults.getOrDefault(f, false);
    }

    private static Set<Material> materials(FileConfiguration c, String path, Logger log) {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String s : c.getStringList(path)) {
            Material m = Material.matchMaterial(s.trim());
            if (m == null) log.warning("Geçersiz materyal (" + path + "): " + s);
            else set.add(m);
        }
        return set;
    }
}
