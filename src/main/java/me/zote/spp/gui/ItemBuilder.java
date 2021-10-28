package me.zote.spp.gui;

import me.zote.spp.gui.buttons.GUIButton;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {

    private ItemStack stack;

    public ItemBuilder(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemBuilder start(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static GUIButton filler(Material material) {
        return filler("&c", material);
    }

    public static GUIButton filler(String name, Material material) {
        return ItemBuilder.start(material).name(name).buildButton();
    }

    public static ItemBuilder start(ItemStack stack) {
        return new ItemBuilder(stack);
    }

    public ItemBuilder name(String name) {
        ItemMeta stackMeta = stack.getItemMeta();
        stackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        stack.setItemMeta(stackMeta);
        return this;
    }

    public ItemBuilder title(String title) {
        return name(title);
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(amount);
        return this;
    }

    public ItemBuilder owner(OfflinePlayer player) {
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(player);
        stack.setItemMeta(meta);
        return this;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder owner(String player) {
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwner(player);
        stack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder texture(String texture) {
        if (texture == null || texture.isEmpty())
            return this;

        stack = Bukkit.getUnsafe().modifyItemStack(stack, "{SkullOwner:{Id:\"179a5541-5448-4b53-ad64-f0144ec2f30e\",Properties:{textures:[{Value:\"" + texture + "\"}]}}}");
        return this;
    }

    public ItemBuilder lore(String... lore) {
        for (int i = 0; i < lore.length; i++) {
            lore[i] = ChatColor.translateAlternateColorCodes('&', lore[i]);
        }

        ItemMeta stackMeta = stack.getItemMeta();
        List<String> newLore = stackMeta.hasLore() ? stackMeta.getLore() : new ArrayList<>();
        newLore.addAll(Arrays.asList(lore));
        stackMeta.setLore(newLore);
        stack.setItemMeta(stackMeta);
        return this;
    }

    public ItemBuilder lore(List<String> add) {
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        add = add.stream().map(this::color).collect(Collectors.toList());
        lore.addAll(add);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return stack;
    }

    public GUIButton buildButton() {
        return new GUIButton(build());
    }

    public String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

}
