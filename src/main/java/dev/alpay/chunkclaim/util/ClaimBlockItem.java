package dev.alpay.chunkclaim.util;

import dev.alpay.chunkclaim.ChunkClaimPlugin;
import dev.alpay.chunkclaim.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

/** Claim yönetim bloğu eşyası: PDC etiketiyle normal bloktan ayrılır. */
public class ClaimBlockItem {

    private final ChunkClaimPlugin plugin;
    private final NamespacedKey tagKey;
    private final NamespacedKey recipeKey;

    public ClaimBlockItem(ChunkClaimPlugin plugin) {
        this.plugin = plugin;
        this.tagKey = new NamespacedKey(plugin, "claim_block");
        this.recipeKey = new NamespacedKey(plugin, "claim_block_recipe");
    }

    public ItemStack create(int amount) {
        Settings s = plugin.settings();
        return ItemBuilder.of(s.claimBlockMaterial())
                .name(plugin.messages().raw("claim-block.name"))
                .lore(plugin.messages().list("claim-block.lore"))
                .glow(true)
                .meta(meta -> meta.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1))
                .amount(amount)
                .build();
    }

    public boolean isClaimBlock(ItemStack item) {
        if (item == null || item.getType() != plugin.settings().claimBlockMaterial()) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(tagKey, PersistentDataType.BYTE);
    }

    public void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);
        Settings s = plugin.settings();
        if (!s.recipeEnabled()) return;
        List<String> shape = s.recipeShape();
        Map<Character, Material> ingredients = s.recipeIngredients();
        if (shape.isEmpty() || ingredients.isEmpty()) {
            plugin.getLogger().warning("Claim block recipe is incomplete — recipe not registered.");
            return;
        }
        try {
            ShapedRecipe recipe = new ShapedRecipe(recipeKey, create(1));
            recipe.shape(shape.toArray(new String[0]));
            for (Map.Entry<Character, Material> e : ingredients.entrySet()) {
                recipe.setIngredient(e.getKey(), new RecipeChoice.MaterialChoice(e.getValue()));
            }
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not register claim block recipe: " + e.getMessage());
        }
    }

    public void unregisterRecipe() {
        Bukkit.removeRecipe(recipeKey);
    }
}
