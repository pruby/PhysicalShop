package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.Localized.Message.*;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import com.wolvereness.physicalshop.exception.InvalidExchangeException;
import com.wolvereness.physicalshop.exception.InvalidSignException;

/**
 *
 */
public class ChestShop extends Shop {
	private final InventoryHolder chest;
	/**
	 * Creates a Shop with a chest
	 * @param sign sign to consider
	 * @param plugin The active PhysicalShop plugin
	 * @throws InvalidSignException thrown if sign is not over a block with inventory or sign is invalid
	 */
	public ChestShop(final Sign sign, final PhysicalShop plugin) throws InvalidSignException {
		super(sign, plugin);

		final Block chestBlock = sign.getBlock().getRelative(BlockFace.DOWN);

		final BlockState chest = chestBlock.getState();
		if (	!(chest instanceof InventoryHolder)
				|| plugin.getPluginConfig().isBlacklistedShopType(chest.getType())
				) throw new InvalidSignException();
		this.chest = (InventoryHolder) chest;
	}
	/**
	 * Creates a Shop with the specified InventoryHolder
	 * @param sign sign to consider
	 * @param plugin The active PhysicalShop plugin
	 * @param inventory The inventory to use
	 * @throws InvalidSignException thrown if the sign is invalid
	 */
	public ChestShop(final Sign sign, final PhysicalShop plugin, final InventoryHolder inventory) throws InvalidSignException {
		super(sign, plugin);

		Validate.notNull(inventory, "Inventory cannot be null");

		if (	inventory instanceof BlockState
				&& plugin.getPluginConfig().isBlacklistedShopType(((BlockState) inventory).getType())
				) throw new InvalidSignException();
		this.chest = inventory;
	}
	@Override
	protected boolean buy(final Player player, final PhysicalShop plugin) {
		if (!canBuy()) {
			plugin.getLocale().sendMessage(player, NO_BUY);
			return false;
		}
		final ShopItemStack[] items = InventoryHelpers.getItems(chest.getInventory());

		try {
			InventoryHelpers.exchange(chest.getInventory(), getBuyCurrency().getStack(getBuyRate().getPrice()), getMaterial().getStack(getBuyRate().getAmount()));
		} catch (final InvalidExchangeException e) {
			switch (e.getType()) {
			case ADD:
				plugin.getLocale().sendMessage(player, CHEST_INVENTORY_FULL);
				break;
			case REMOVE:
				plugin.getLocale().sendMessage(
					player,
					NOT_ENOUGH_SHOP_ITEMS,
					getMaterial().toString(plugin.getMaterialConfig())
					);
				break;
			}

			return false;
		}

		if (!super.buy(player, plugin)) {
			InventoryHelpers.setItems(chest.getInventory(), items);
			return false;
		}
		return true;
	}
	@Override
	/**
	 * Gets the current amount of shop's currency in the chest.
	 * @return
	 */
	public int getShopBuyCapital() {
		return InventoryHelpers.getCount(chest.getInventory(), getBuyCurrency());
	}
	@Override
	public int getShopItems() {
		return InventoryHelpers.getCount(chest.getInventory(), getMaterial());
	}
	@Override
	/**
	 * Gets the current amount of shop's currency in the chest.
	 * @return
	 */
	public int getShopSellCapital() {
		return InventoryHelpers.getCount(chest.getInventory(), getSellCurrency());
	}
	@Override
	public boolean isShopBlock(final Block block) {
		if (super.isShopBlock(block)) return true;

		return 	chest instanceof BlockState
				&& ((BlockState) chest).getBlock().equals(block);
	}
	@Override
	public boolean sell(final Player player, final PhysicalShop plugin) {
		if (!canSell()) {
			plugin.getLocale().sendMessage(player, NO_SELL);
			return false;
		}
		final ShopItemStack[] items = InventoryHelpers.getItems(chest.getInventory());

		try {
			InventoryHelpers.exchange(
				chest.getInventory(),
				getMaterial().getStack(getSellRate().getAmount()),
				getSellCurrency().getStack(getSellRate().getPrice())
				);
		} catch (final InvalidExchangeException e) {
			switch (e.getType()) {
			case ADD:
				plugin.getLocale().sendMessage(player, CHEST_INVENTORY_FULL);
				break;
			case REMOVE:
				plugin.getLocale().sendMessage(player, NOT_ENOUGH_SHOP_MONEY, getSellCurrency().toString(plugin.getMaterialConfig()));
				break;
			}

			return false;
		}

		if (!super.sell(player, plugin)) {
			InventoryHelpers.setItems(chest.getInventory(), items);
			return false;
		}
		return true;
	}
	@Override
	public void status(final Player p, final PhysicalShop plugin) {
		if (!plugin.getPluginConfig().isDetailedOutput()) {
			if (!canSell()) {
				plugin.getLocale().sendMessage(
					p,
					STATUS_ONE_MATERIAL,
					getShopItems(),
					getMaterial().toString(plugin.getMaterialConfig())
					);
			} else if (!canBuy()) {
				plugin.getLocale().sendMessage(
					p,
					STATUS_ONE_MATERIAL,
					getShopSellCapital(),
					getSellCurrency().toString(plugin.getMaterialConfig())
					);
			} else {
				plugin.getLocale().sendMessage(
					p,
					STATUS_ONE_CURRENCY,
					getShopSellCapital(),
					getSellCurrency().toString(plugin.getMaterialConfig()),
					getShopItems(),
					getMaterial().toString(plugin.getMaterialConfig())
					);
			}
		} else if (!canBuy()) {
			plugin.getLocale().sendMessage(
				p,
				STATUS_ONE_CURRENCY,
				getShopSellCapital(),
				getSellCurrency().toString(plugin.getMaterialConfig()),
				getShopItems(),
				getMaterial().toString(plugin.getMaterialConfig())
				);
		} else if (!canSell() || getSellCurrency().equals(getBuyCurrency())) {
			plugin.getLocale().sendMessage(
				p,
				STATUS_ONE_CURRENCY,
				getShopBuyCapital(),
				getBuyCurrency().toString(plugin.getMaterialConfig()),
				getShopItems(),
				getMaterial().toString(plugin.getMaterialConfig())
				);
		} else {
			plugin.getLocale().sendMessage(
				p,
				STATUS,
				getShopBuyCapital(),
				getBuyCurrency().toString(plugin.getMaterialConfig()),
				getShopSellCapital(),
				getSellCurrency().toString(plugin.getMaterialConfig()),
				getShopItems(),
				getMaterial().toString(plugin.getMaterialConfig())
				);
		}

		super.status(p, plugin);
	}

}
