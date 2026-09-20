package dev.alpay.chunkclaim.config;

import dev.alpay.chunkclaim.claim.ClaimFlag;
import dev.alpay.chunkclaim.claim.MemberPermission;
import dev.alpay.chunkclaim.claim.UpgradeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dil dosyası okuyucusu (lang/&lt;kod&gt;.yml) + MiniMessage yardımcıları.
 * Eksik anahtarlar: seçilen dilin jar içindeki kopyası → jar içindeki tr.yml.
 */
public class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String[] BUNDLED = {"tr", "en"};

    private final JavaPlugin plugin;
    private YamlConfiguration config;
    private String prefix;
    private String language;

    public Messages(JavaPlugin plugin, String language) {
        this.plugin = plugin;
        reload(language);
    }

    public void reload(String language) {
        this.language = language;
        File dir = new File(plugin.getDataFolder(), "lang");
        if (!dir.exists() && !dir.mkdirs()) plugin.getLogger().warning("lang klasörü oluşturulamadı");
        for (String code : BUNDLED) {
            if (!new File(dir, code + ".yml").exists()) plugin.saveResource("lang/" + code + ".yml", false);
        }

        File file = new File(dir, language + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Dil dosyası bulunamadı: " + file.getName() + " — 'tr' kullanılıyor");
            this.language = "tr";
            file = new File(dir, "tr.yml");
        }
        config = YamlConfiguration.loadConfiguration(file);

        // Fallback zinciri: jar'daki aynı dil → jar'daki tr
        YamlConfiguration bundledSame = bundled(this.language);
        YamlConfiguration bundledTr = bundled("tr");
        if (bundledSame != null) {
            if (bundledTr != null && !this.language.equals("tr")) bundledSame.setDefaults(bundledTr);
            config.setDefaults(bundledSame);
        } else if (bundledTr != null) {
            config.setDefaults(bundledTr);
        }
        prefix = config.getString("prefix", "<green>Claim <gray>» ");
    }

    private YamlConfiguration bundled(String code) {
        InputStream in = plugin.getResource("lang/" + code + ".yml");
        if (in == null) return null;
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public String language() {
        return language;
    }

    public static MiniMessage mm() {
        return MM;
    }

    // ---------- Statik yardımcılar ----------

    public static Component parse(String raw, Map<String, String> placeholders) {
        return MM.deserialize(replace(raw, placeholders));
    }

    public static Component parse(String raw) {
        return MM.deserialize(raw);
    }

    /** Menü lore/isimlerinde italic'i kapatmak için. */
    public static Component item(String raw, Map<String, String> placeholders) {
        return MM.deserialize("<!italic>" + replace(raw, placeholders));
    }

    public static Component item(String raw) {
        return item(raw, Map.of());
    }

    public static String replace(String raw, Map<String, String> placeholders) {
        if (raw == null) return "";
        String out = raw;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    // ---------- Anahtar erişimi ----------

    public String raw(String key) {
        String v = config.getString(key);
        return v == null ? "<red>[" + key + "]" : v;
    }

    /** Placeholder uygulanmış ham string (MiniMessage parse edilmemiş). */
    public String raw(String key, Map<String, String> placeholders) {
        return replace(raw(key), placeholders);
    }

    public List<String> list(String key) {
        return config.getStringList(key);
    }

    public Component get(String key) {
        return get(key, Map.of());
    }

    public Component get(String key, Map<String, String> placeholders) {
        return parse(prefix + raw(key), placeholders);
    }

    /** Prefix'siz. */
    public Component plain(String key, Map<String, String> placeholders) {
        return parse(raw(key), placeholders);
    }

    public void send(CommandSender to, String key) {
        to.sendMessage(get(key));
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        to.sendMessage(get(key, placeholders));
    }

    public List<Component> items(List<String> raws, Map<String, String> placeholders) {
        List<Component> out = new ArrayList<>(raws.size());
        for (String r : raws) out.add(item(r, placeholders));
        return out;
    }

    public String state(boolean on) {
        return raw(on ? "state-on" : "state-off");
    }

    // ---------- Enum isimleri ----------

    public String flagName(ClaimFlag flag) {
        return raw("flags." + flag.name());
    }

    public String upgradeName(UpgradeType type) {
        return raw("upgrades." + type.key() + ".name");
    }

    public String upgradeDescription(UpgradeType type) {
        return raw("upgrades." + type.key() + ".description");
    }

    public String permissionName(MemberPermission perm) {
        return raw("permissions." + perm.name());
    }
}
