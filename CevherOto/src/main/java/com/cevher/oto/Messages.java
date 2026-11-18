package com.cevher.oto;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/** & renk kodlu (ve &#RRGGBB hex) mesajları Component'e çevirir. */
public final class Messages {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private FileConfiguration config;

    public void reload(FileConfiguration config) {
        this.config = config;
    }

    public String raw(String path) {
        return config.getString(path, "");
    }

    public static Component parse(String s) {
        return AMP.deserialize(s == null ? "" : s);
    }

    /** Menü eşyaları için: italik kapalı. */
    public static Component item(String s) {
        return parse(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> itemLines(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(item(l));
        return out;
    }

    /** PlaceholderAPI için § kodlu metin. */
    public static String toSection(String amp) {
        return SECTION.serialize(parse(amp));
    }

    public String featureName(Feature f) {
        return config.getString("ozellikler." + f.id + ".isim", f.id);
    }

    public String state(boolean on) {
        return raw(on ? "mesajlar.durum-acik" : "mesajlar.durum-kapali");
    }

    /** mesajlar.<key> değerini önekle gönderir. repl: "{anahtar}", "değer" çiftleri. */
    public void send(CommandSender to, String key, String... repl) {
        String text = raw("mesajlar." + key);
        if (text.isEmpty()) return;
        for (int i = 0; i + 1 < repl.length; i += 2) text = text.replace(repl[i], repl[i + 1]);
        to.sendMessage(parse(raw("mesajlar.onek") + text));
    }
}
