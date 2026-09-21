package dev.alpay.chunkclaim.config;

import dev.alpay.chunkclaim.claim.Claim;
import dev.alpay.chunkclaim.claim.UpgradeType;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** config.yml'nin tip güvenli okuyucusu. reload() ile yeniden yüklenir. Tüm metinler lang dosyasındadır. */
public class Settings {

    public enum EconomyType { VAULT, DIAMOND, AUTO }

    /** AUTO: kare claim → world border, diğerleri → partikül. */
    public enum BorderMode { AUTO, WORLD_BORDER, PARTICLES }

    private static final Pattern DEFAULT_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} _'\\-.!?]+$");

    private final JavaPlugin plugin;
    private final Logger log;

    private String language;

    private Material claimBlockMaterial;
    private boolean recipeEnabled;
    private List<String> recipeShape;
    private Map<Character, Material> recipeIngredients;

    private int maxClaimsPerPlayer;
    private List<String> allowedWorlds;
    private int minDistanceChunks;

    private EconomyType economyType;
    private boolean diamondAcceptBlocks;
    private double chunkPriceBase;
    private double chunkPricePerChunk;
    private double diamondChunkPriceBase;
    private double diamondChunkPricePerChunk;
    private double unclaimRefund;
    private double deleteRefund;

    private final Map<UpgradeType, Integer> upgradeBase = new EnumMap<>(UpgradeType.class);
    private final Map<UpgradeType, List<UpgradeLevel>> upgradeLevels = new EnumMap<>(UpgradeType.class);

    private boolean hologramEnabled;
    private double hologramYOffset;
    private int hologramUpdateInterval;

    private BorderMode borderMode;
    private Color borderParticleColor;
    private long borderExpandMs;
    private long borderHoldMs;
    private long borderShrinkMs;
    private boolean borderShowOnEnter;
    private double borderAutoHideDistance;

    private boolean creationAskName;
    private int maxNameLength;
    private Pattern namePattern;

    private int teleportWarmup;
    private boolean teleportCancelOnMove;
    private boolean teleportCancelOnDamage;
    private int teleportCooldown;
    private double teleportCost;
    private double teleportDiamondCost;

    private boolean titlesEnabled;
    private int chatPromptTimeout;

    public Settings(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        language = c.getString("language", "en").toLowerCase(Locale.ROOT);

        claimBlockMaterial = material(c.getString("claim-block.material", "LODESTONE"), Material.LODESTONE);
        recipeEnabled = c.getBoolean("claim-block.recipe.enabled", true);
        recipeShape = c.getStringList("claim-block.recipe.shape");
        recipeIngredients = new HashMap<>();
        ConfigurationSection ing = c.getConfigurationSection("claim-block.recipe.ingredients");
        if (ing != null) {
            for (String key : ing.getKeys(false)) {
                if (key.length() != 1) continue;
                Material m = material(ing.getString(key), null);
                if (m != null) recipeIngredients.put(key.charAt(0), m);
            }
        }

        maxClaimsPerPlayer = c.getInt("limits.max-claims-per-player", 1);
        allowedWorlds = c.getStringList("limits.allowed-worlds");
        minDistanceChunks = c.getInt("limits.min-distance-chunks", 0);

        String eco = c.getString("economy.type", "AUTO").toUpperCase(Locale.ROOT);
        try {
            economyType = EconomyType.valueOf(eco);
        } catch (IllegalArgumentException e) {
            log.warning("Invalid economy.type: " + eco + " — using AUTO");
            economyType = EconomyType.AUTO;
        }
        diamondAcceptBlocks = c.getBoolean("economy.diamond.accept-blocks", true);
        chunkPriceBase = c.getDouble("economy.chunk-price.vault.base", 100);
        chunkPricePerChunk = c.getDouble("economy.chunk-price.vault.per-chunk", 50);
        diamondChunkPriceBase = c.getDouble("economy.chunk-price.diamond.base", 2);
        diamondChunkPricePerChunk = c.getDouble("economy.chunk-price.diamond.per-chunk", 1);
        unclaimRefund = Math.max(0, Math.min(1, c.getDouble("economy.unclaim-refund", 0.5)));
        deleteRefund = Math.max(0, Math.min(1, c.getDouble("economy.delete-refund", unclaimRefund)));

        upgradeBase.clear();
        upgradeLevels.clear();
        for (UpgradeType type : UpgradeType.values()) {
            String path = "upgrades." + type.key();
            upgradeBase.put(type, c.getInt(path + ".base", 0));
            List<UpgradeLevel> levels = new ArrayList<>();
            List<Map<?, ?>> raw = c.getMapList(path + ".levels");
            for (Map<?, ?> m : raw) {
                double cost = m.get("cost") instanceof Number n ? n.doubleValue() : 0;
                // diamond verilmemişse Vault fiyatını kullan (eski config uyumluluğu)
                double diamond = m.get("diamond") instanceof Number n ? n.doubleValue() : cost;
                int value = m.get("value") instanceof Number n ? n.intValue() : 0;
                levels.add(new UpgradeLevel(cost, diamond, value));
            }
            upgradeLevels.put(type, Collections.unmodifiableList(levels));
        }

        hologramEnabled = c.getBoolean("hologram.enabled", true);
        hologramYOffset = c.getDouble("hologram.y-offset", 1.6);
        hologramUpdateInterval = Math.max(20, c.getInt("hologram.update-interval-ticks", 100));

        String bm = c.getString("border.mode", "AUTO").toUpperCase(Locale.ROOT);
        try {
            borderMode = BorderMode.valueOf(bm);
        } catch (IllegalArgumentException e) {
            log.warning("Invalid border.mode: " + bm + " — using AUTO");
            borderMode = BorderMode.AUTO;
        }
        borderParticleColor = parseColor(c.getString("border.particle-color", "#55FF55"), Color.LIME);
        borderExpandMs = c.getLong("border.expand-ms", 1500);
        borderHoldMs = c.getLong("border.hold-ms", 4000);
        borderShrinkMs = c.getLong("border.shrink-ms", 1000);
        borderShowOnEnter = c.getBoolean("border.show-on-enter", false);
        borderAutoHideDistance = Math.max(0, c.getDouble("border.auto-hide-distance", 2.0));

        creationAskName = c.getBoolean("creation.ask-name", true);
        maxNameLength = Math.max(3, c.getInt("creation.max-name-length", 24));
        String pattern = c.getString("creation.name-pattern", DEFAULT_NAME_PATTERN.pattern());
        try {
            namePattern = Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            log.warning("Invalid creation.name-pattern, using default: " + e.getDescription());
            namePattern = DEFAULT_NAME_PATTERN;
        }

        teleportWarmup = Math.max(0, c.getInt("teleport.warmup-seconds", 3));
        teleportCancelOnMove = c.getBoolean("teleport.cancel-on-move", true);
        teleportCancelOnDamage = c.getBoolean("teleport.cancel-on-damage", true);
        teleportCooldown = Math.max(0, c.getInt("teleport.cooldown-seconds", 30));
        teleportCost = Math.max(0, c.getDouble("teleport.cost", 0));
        teleportDiamondCost = Math.max(0, c.getDouble("teleport.diamond-cost", 0));

        titlesEnabled = c.getBoolean("titles.enabled", true);
        chatPromptTimeout = Math.max(10, c.getInt("chat-prompt-timeout-seconds", 60));
    }

    private Color parseColor(String hex, Color def) {
        if (hex == null) return def;
        String h = hex.trim().replace("#", "");
        if (h.length() != 6) {
            log.warning("Invalid color: " + hex);
            return def;
        }
        try {
            return Color.fromRGB(Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            log.warning("Invalid color: " + hex);
            return def;
        }
    }

    private Material material(String name, Material def) {
        if (name == null) return def;
        Material m = Material.matchMaterial(name);
        if (m == null) {
            log.warning("Unknown material: " + name);
            return def;
        }
        return m;
    }

    // --- Geliştirme yardımcıları ---

    public int upgradeBase(UpgradeType type) {
        return upgradeBase.getOrDefault(type, 0);
    }

    public List<UpgradeLevel> upgradeLevels(UpgradeType type) {
        return upgradeLevels.getOrDefault(type, List.of());
    }

    public int maxUpgradeLevel(UpgradeType type) {
        return upgradeLevels(type).size();
    }

    /** Claim'in mevcut seviyesindeki değeri döner (seviye 0 = base). */
    public int upgradeValue(Claim claim, UpgradeType type) {
        int level = claim.getUpgradeLevel(type);
        List<UpgradeLevel> levels = upgradeLevels(type);
        if (level <= 0 || levels.isEmpty()) return upgradeBase(type);
        return levels.get(Math.min(level, levels.size()) - 1).value();
    }

    /** Sonraki seviye, yoksa null. */
    public UpgradeLevel nextUpgrade(Claim claim, UpgradeType type) {
        int level = claim.getUpgradeLevel(type);
        List<UpgradeLevel> levels = upgradeLevels(type);
        if (level >= levels.size()) return null;
        return levels.get(level);
    }

    /** Aktif ekonomiye göre chunk fiyatı: fiyat = base + (mevcutChunk - 1) * per-chunk */
    public double chunkPrice(int currentChunks, boolean diamond) {
        double base = diamond ? diamondChunkPriceBase : chunkPriceBase;
        double per = diamond ? diamondChunkPricePerChunk : chunkPricePerChunk;
        return base + Math.max(0, currentChunks - 1) * per;
    }

    /** Aktif ekonomiye göre seviye fiyatı. */
    public double upgradeCost(UpgradeLevel level, boolean diamond) {
        return diamond ? level.diamondCost() : level.cost();
    }

    // --- Getters ---

    public String language() { return language; }
    public Material claimBlockMaterial() { return claimBlockMaterial; }
    public boolean recipeEnabled() { return recipeEnabled; }
    public List<String> recipeShape() { return recipeShape; }
    public Map<Character, Material> recipeIngredients() { return recipeIngredients; }
    public int maxClaimsPerPlayer() { return maxClaimsPerPlayer; }
    public List<String> allowedWorlds() { return allowedWorlds; }
    public int minDistanceChunks() { return minDistanceChunks; }
    public EconomyType economyType() { return economyType; }
    public boolean diamondAcceptBlocks() { return diamondAcceptBlocks; }
    public double unclaimRefund() { return unclaimRefund; }
    public double deleteRefund() { return deleteRefund; }
    public boolean hologramEnabled() { return hologramEnabled; }
    public double hologramYOffset() { return hologramYOffset; }
    public int hologramUpdateInterval() { return hologramUpdateInterval; }
    public BorderMode borderMode() { return borderMode; }
    public Color borderParticleColor() { return borderParticleColor; }
    public long borderExpandMs() { return borderExpandMs; }
    public long borderHoldMs() { return borderHoldMs; }
    public long borderShrinkMs() { return borderShrinkMs; }
    public boolean borderShowOnEnter() { return borderShowOnEnter; }
    public double borderAutoHideDistance() { return borderAutoHideDistance; }
    public boolean creationAskName() { return creationAskName; }
    public int maxNameLength() { return maxNameLength; }
    public Pattern namePattern() { return namePattern; }
    public int teleportWarmup() { return teleportWarmup; }
    public boolean teleportCancelOnMove() { return teleportCancelOnMove; }
    public boolean teleportCancelOnDamage() { return teleportCancelOnDamage; }
    public int teleportCooldown() { return teleportCooldown; }
    public double teleportCost(boolean diamond) { return diamond ? teleportDiamondCost : teleportCost; }
    public boolean titlesEnabled() { return titlesEnabled; }
    public int chatPromptTimeout() { return chatPromptTimeout; }
}
