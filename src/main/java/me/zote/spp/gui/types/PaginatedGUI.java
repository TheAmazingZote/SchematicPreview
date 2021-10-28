package me.zote.spp.gui.types;

import me.zote.spp.gui.ItemBuilder;
import me.zote.spp.gui.Paginator;
import me.zote.spp.gui.buttons.GUIButton;
import me.zote.spp.gui.buttons.InventoryListenerGUI;
import me.zote.spp.gui.config.GUIConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public class PaginatedGUI implements InventoryHolder {

	private final Paginator<GUIButton> paginator = new Paginator<>(45);
	private final Map<Integer, GUIButton> items = new ConcurrentHashMap<>();
	private final Map<Integer, GUIButton> paginationItems = new HashMap<>();
	private final Map<Integer, GUIButton> toolbarItems = new HashMap<>();
	private static InventoryListenerGUI inventoryListenerGUI;
	private final GUIConfig configuration;
	private String name;

	public PaginatedGUI(String name) {
		this(name, new GUIConfig());
	}

	public PaginatedGUI(String name, GUIConfig config) {
		this.configuration = config;
		this.name = name;
	}

	public static void prepare(JavaPlugin plugin) {
		if (inventoryListenerGUI == null) {
			inventoryListenerGUI = new InventoryListenerGUI();
			plugin.getServer().getPluginManager().registerEvents(inventoryListenerGUI, plugin);
		}
	}

	public String getDisplayName() {
		return name;
	}

	public void setDisplayName(String name) {
		this.name = color(name);
	}

	public boolean hasToolbar() {
		return !toolbarItems.isEmpty();
	}

	public GUIButton getButton(int slot) {
		if (configuration != null && configuration.isPagination()) {
			if (!paginationItems.isEmpty()) {
				if (slot / 9 == (getSize() - 1) / 9)
					return paginationItems.get(slot % 9);
			}
			return paginator.getPage().get(slot);
		}

		if (hasToolbar()) {
			if (slot / 9 == (getSize() - 1) / 9)
				return toolbarItems.get(slot % 9);
		}
		return items.get(slot);
	}

	public void fill(GUIButton button) {
		for (int slot = 0; slot < getSize(); slot++)
			if (!items.containsKey(slot))
				setButton(slot, button);
	}

	public void fillRow(int row, GUIButton button) {
		if (configuration != null && configuration.isPagination())
			throw new UnsupportedOperationException("This action is not available for paginated gui's");

		if (row < 0 || row > 5) {
			throw new IllegalArgumentException(
					"The desired row is outside the bounds of the inventory slot range. [0-5]");
		}
		int startSlot = row * 9;
		for (int i = 0; i < 9; i++) {
			if (getButton(startSlot + i) == null)
				setButton(startSlot + i, button);
		}
	}

	public void setRow(int row, GUIButton button) {
		if (configuration != null && configuration.isPagination())
			throw new UnsupportedOperationException("This action is not available for paginated gui's");
		if (row < 0 || row > 5) {
			throw new IllegalArgumentException(
					"The desired row is outside the bounds of the inventory slot range. [0-5]");
		}
		int startSlot = row * 9;
		for (int i = 0; i < 9; i++)
			setButton(startSlot + i, button);
	}

	public void addButton(GUIButton button) {
		if (configuration != null && configuration.isPagination()) {
			this.paginator.addElement(button);
			return;
		}

		for (int slot = 0; slot < getSize(); slot++) {
			if (!items.containsKey(slot)) {
				items.put(slot, button);
				break;
			}
		}
	}

	public void setButton(int slot, GUIButton button) {
		if (configuration != null && configuration.isPagination()) {
			this.paginator.addElement(button);
			return;
		}
		items.put(slot, button);
	}

	public void removeButton(int slot) {
		if (configuration != null && configuration.isPagination())
			throw new UnsupportedOperationException("This action is not available for paginated gui's");
		items.remove(slot);
	}

	public void fillToolbar(GUIButton button) {
		for (int i = 0; i < 9; i++)
			if (!toolbarItems.containsKey(i))
				toolbarItems.put(i, button);
	}

	public void setToolbarItem(int slot, GUIButton button) {
		if (slot < 0 || slot > 8) {
			throw new IllegalArgumentException(
					"The desired slot is outside the bounds of the toolbar slot range. [0-8]");
		}

		toolbarItems.put(slot, button);
	}

	public void removeToolbarItem(int slot) {
		if (slot < 0 || slot > 8) {
			throw new IllegalArgumentException(
					"The desired slot is outside the bounds of the toolbar slot range. [0-8]");
		}

		toolbarItems.remove(slot);
	}

	public void refreshInventory(HumanEntity holder) {
		holder.openInventory(getInventory());
	}

	public boolean setToolbar(Inventory inv) {
		paginationItems.clear();
		GUIButton backButton = new GUIButton(
				ItemBuilder.start(Material.ARROW).name(configuration.getPreviousPage()).build());
		GUIButton pageIndicator = new GUIButton(ItemBuilder.start(Material.NAME_TAG)
				.name(configuration.getCurrentPage()
						.replaceAll(Pattern.quote("{currentPage}"), String.valueOf(paginator.getCurrent()))
						.replaceAll(Pattern.quote("{maxPages}"), String.valueOf(paginator.getTotalPages())))
				.build());
		GUIButton nextButton = new GUIButton(
				ItemBuilder.start(Material.ARROW).name(configuration.getNextPage()).build());

		backButton.setListener(event -> {
			PaginatedGUI menu = (PaginatedGUI) event.getClickedInventory().getHolder();
			menu.paginator.setCurrent(menu.paginator.getPrev());
			refreshInventory(event.getWhoClicked());
		});

		nextButton.setListener(event -> {
			PaginatedGUI menu = (PaginatedGUI) event.getClickedInventory().getHolder();

			menu.paginator.setCurrent(menu.paginator.getNext());
			refreshInventory(event.getWhoClicked());
		});

		paginationItems.putAll(toolbarItems);
		int size = getSize() - 9;
		if (configuration.isPagination()) {
			if (paginator.getTotalPages() <= 1)
				return false;
			if (paginator.hasPrev()) {
				inv.setItem(size + 3, backButton.getItem());
				paginationItems.put(3, backButton);
			}

			inv.setItem(size + 4, pageIndicator.getItem());
			paginationItems.put(4, pageIndicator);

			if (paginator.hasNext()) {
				inv.setItem(size + 5, nextButton.getItem());
				paginationItems.put(5, nextButton);
			}
			return true;
		}

		return false;

	}

	@Override
	public Inventory getInventory() {
		int size = getSize();
		Inventory inventory = Bukkit.createInventory(this, size, name);
		int minus = hasToolbar() ? 9 : 0;
		size -= minus;

		if (configuration != null && configuration.isPagination()) {
			List<GUIButton> butts = paginator.getPage();
			for (int i = 0; i < paginator.getPage().size(); i++)
				inventory.setItem(i, butts.get(i).getItem());
		} else {
			for (Entry<Integer, GUIButton> ent : items.entrySet()) {
				inventory.setItem(ent.getKey(), ent.getValue().getItem());
			}
		}

		if (!setToolbar(inventory)) {
			if (hasToolbar()) {
				for (Entry<Integer, GUIButton> ent : toolbarItems.entrySet()) {
					int rawSlot = ent.getKey() + size;
					inventory.setItem(rawSlot, ent.getValue().getItem());
				}
			}
		}

		return inventory;
	}

	public int getSize() {
		if (configuration != null && configuration.isPagination()) {
			int plus = paginator.getTotalPages() == 0 ? 0 : 9;
			return getSize(paginator.getPage().size()) + plus;
		}

		int plus = hasToolbar() ? 9 : 0;
		int size = configuration.getSize();
		int itemSize = items.isEmpty() ? 9 : items.keySet().stream().mapToInt(i -> i).max().getAsInt();
		itemSize = getSize(itemSize);
		if (size == 0)
			return itemSize + plus;
		return size + plus;
	}

	public int getSize(int size) {
		if (size <= 0)
			size = 9;
		double d = Math.ceil(size / 9.0);
		if (d <= 0.0)
			d = 1.0;
		return (int) (d * 9);
	}

	private String color(String toColor) {
		return ChatColor.translateAlternateColorCodes('&', toColor);
	}

}