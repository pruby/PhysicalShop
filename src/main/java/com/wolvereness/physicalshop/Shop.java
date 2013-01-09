package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.ConfigOptions.TRIGGER_REDSTONE;
import static com.wolvereness.physicalshop.config.Localized.Message.*;
import static java.util.logging.Level.SEVERE;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_4_6.CraftChunk;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.Attachable;

import com.wolvereness.physicalshop.config.MaterialConfig;
import com.wolvereness.physicalshop.exception.InvalidExchangeException;
import com.wolvereness.physicalshop.exception.InvalidMaterialException;
import com.wolvereness.physicalshop.exception.InvalidSignException;
import com.wolvereness.physicalshop.exception.InvalidSignOwnerException;
import com.wolvereness.util.NameCollection;

/**
 *
 */
public class Shop {
	/**
	 * Figures out the current material the shop uses.
	 * @param lines text from sign
	 * @param config The MaterialConfig to use
	 * @return an associated shop material, or null if failed to decypher
	 */
	public static ShopMaterial getMaterial(final String[] lines, final MaterialConfig config) {
		try {
			return config.getShopMaterial(lines[0]);
		} catch (final InvalidMaterialException e) {
			return null;
		}
	}
	/**
	 * Owner is found on fourth line of sign. This will NOT cross-check for extended player names!
	 * @param lines The set of lines associated with the sign for a shop.
	 * @return name of the owner of said shop
	 */
	public static String getOwnerName(final String[] lines) {
		return lines[3];
	}
	@SuppressWarnings("deprecation")
	private static void updateInventory(final Player player) {
		player.updateInventory();
	}
	private final Rate buyRate;
	private int hash;
	private final ShopMaterial material;
	private final String ownerName;
	private final Rate sellRate;
	private final Sign sign;
	/**
	 * Initializes Shop based off of sign.
	 * @param sign the sign to consider
	 * @param plugin The active PhysicalShop plugin
	 * @throws InvalidSignException If sign does not match correct pattern.
	 */
	public Shop(final Sign sign, final PhysicalShop plugin) throws InvalidSignException {
		this(sign.getLines(), plugin, sign);
	}
	/**
	 * Initializes a shop based off the lines from a sign. Used to check validity.
	 * @param lines the text from the sign to consider
	 * @param plugin The active PhysicalShop plugin
	 * @throws InvalidSignException If the sign text does not match correct pattern.
	 */
	public Shop(final String[] lines, final PhysicalShop plugin) throws InvalidSignException {
		this(lines, plugin, null);
	}
	/**
	 * Initializes a shop based off the lines from a sign. Used to check validity.
	 * @param lines the text from the sign to consider
	 * @param plugin The active PhysicalShop plugin
	 * @param sign the sign to consider
	 * @throws InvalidSignException If the sign text does not match correct pattern.
	 */
	private Shop(final String[] lines, final PhysicalShop plugin, final Sign sign) throws InvalidSignException {
		this.sign = sign;
		material = getMaterial(lines, plugin.getMaterialConfig());

		if (material == null) throw new InvalidSignException();

		buyRate = plugin.getPluginConfig().getBuyPatternHandler().getRate(lines[1], plugin);
		sellRate = plugin.getPluginConfig().getSellPatternHandler().getRate(lines[2], plugin);

		if (buyRate == null && sellRate == null) throw new InvalidSignException();

		if (((this.ownerName = lines[3]) == null) || ownerName.length() == 0) throw new InvalidSignOwnerException();
	}
	/**
	 * Invokes the buy routine for player.
	 * @param player player purchasing
	 * @param plugin The active PhysicalShop plugin
	 * @return true if success
	 */
	protected boolean buy(final Player player, final PhysicalShop plugin) {
		if (!canBuy()) {
			plugin.getLocale().sendMessage(player, NO_BUY);
			return false;
		}

		final Inventory inventory = player.getInventory();

		final int price = getBuyRate().getPrice();
		final int amount = getBuyRate().getAmount();

		try {
			InventoryHelpers.exchange(inventory, material.getStack(amount), getBuyCurrency().getStack(price));
		} catch (final InvalidExchangeException e) {
			switch (e.getType()) {
			case ADD:
				plugin.getLocale().sendMessage(player, PLAYER_INVENTORY_FULL);
				break;
			case REMOVE:
				plugin.getLocale().sendMessage(player, NOT_ENOUGH_PLAYER_MONEY, getBuyCurrency().toString(plugin.getMaterialConfig()));
				break;
			}

			return false;
		}

		plugin.getLocale().sendMessage(
			player,
			BUY,
			amount,
			material.toString(plugin.getMaterialConfig()),
			price,
			getBuyCurrency().toString(plugin.getMaterialConfig())
			);
		updateInventory(player);

		queryLogBlock(player, false, plugin);
		return true;
	}
	/**
	 * @return true if and only if this shop supports buying
	 */
	public boolean canBuy() {
		return buyRate != null;
	}
	/**
	 * @return true if and only if this shop supports selling
	 */
	public boolean canSell() {
		return sellRate != null;
	}
	@Override
	public boolean equals(final Object o) {
		if(o == this) return true;
		if(o == null || !o.getClass().isAssignableFrom(Shop.class)) return false;
		final Shop that = (Shop) o;
		if(that.sign == this.sign) {
			if(this.sign != null) return true; // It's the same sign, so same shop
			return // Signs are null
				(this.buyRate == null
					? that.buyRate == null
					: this.buyRate.equals(that.buyRate))
				&& (this.sellRate == null
					? that.sellRate == null
					: this.sellRate.equals(that.sellRate))
				&& this.material.equals(that.material)
				&& this.ownerName.equals(that.ownerName)
				;
		}
		if(this.sign == null) return false; // this is null, that is not
		return this.sign.equals(that.sign);
	}
	/**
	 * @return the currency associated with buying
	 */
	public ShopMaterial getBuyCurrency() {
		if(!canBuy()) return null;
		return buyRate.getMaterial();
	}
	/**
	 * @return the rate associated with buying
	 */
	public Rate getBuyRate() {
		return buyRate;
	}
	/**
	 * @return the material associated with this shop
	 */
	public ShopMaterial getMaterial() {
		return material;
	}
	/**
	 * @return the owner of this shop
	 */
	public String getOwnerName() {
		return ownerName;
	}
	/**
	 * @return the currency associated with selling
	 */
	public ShopMaterial getSellCurrency() {
		if(!canSell()) return null;
		return sellRate.getMaterial();
	}
	/**
	 * @return the rate associated with selling
	 */
	public Rate getSellRate() {
		return sellRate;
	}
	/**
	 * Gets the current amount of shop's buying currency in the chest.
	 *
	 * @return an amount of currency in this shop from purchases
	 */
	public int getShopBuyCapital() {
		return Integer.MAX_VALUE;
	}
	/**
	 * @return the amount of the shop's material currently stored
	 */
	public int getShopItems() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Gets the current amount of shop's selling currency in the chest.
	 *
	 * @return the amount of currency in this shop for selling
	 */
	public int getShopSellCapital() {
		return Integer.MAX_VALUE;
	}
	/**
	 * @return the associated sign
	 */
	public Sign getSign() {
		return sign;
	}
	@Override
	public int hashCode() {
		if(hash == 0 && sign != null) {
			final Block block = sign.getBlock();
			hash = block.getWorld().hashCode();
			hash = hash * 17 ^ block.getY();
			hash = hash * 19 ^ block.getX();
			hash = hash * 23 ^ block.getZ();
		}
		return hash;
	}
	/**
	 * This method is called when a player right-clicks the sign. It considers the item in player's hand, and will act accordingly.
	 * @param player the player to consider
	 * @param plugin The active PhysicalShop plugin
	 */
	public void interact(final Player player, final PhysicalShop plugin) {
		final ShopMaterial item = new ShopMaterial(player.getItemInHand());
		try {
			if (item.equals(getBuyCurrency())) {
				if(buy(player, plugin)) {
					triggerRedstone(plugin);
				}
			} else if (item.equals(material)) {
				if(sell(player, plugin)) {
					triggerRedstone(plugin);
				}
			}
		} catch (final Throwable t) {
			plugin.getLogger().log(SEVERE, "A problem has occured, please copy and report this entire stacktrace to the author(s)", t);
		}
	}
	/**
	 * @param block block to consider
	 * @return true if said block is the sign for this chest or the sign for this shop is attached to said block
	 */
	public boolean isShopBlock(final Block block) {
		final Block signBlock = sign.getBlock();

		if (block.equals(signBlock)) return true;

		final org.bukkit.material.Sign signData = (org.bukkit.material.Sign) sign.getData();

		return block.equals(signBlock.getRelative(signData.getAttachedFace()));
	}
	/**
	 * @param player Player to check
	 * @param plugin PhysicalShop currently active
	 * @return The owner of the shop, after a name checking if applicable
	 */
	public boolean isSmartOwner(final String player, final PhysicalShop plugin) {
		return plugin.getPluginConfig().isExtendedNames()
			? NameCollection.matches(ownerName, player)
			: ownerName.equals(player);
	}
	private void queryLogBlock(final Player player, final boolean selling, final PhysicalShop plugin) {
		if (plugin.getLogBlock() == null) return;
		final Location chestLocation = sign.getBlock().getRelative(BlockFace.DOWN).getLocation();
		final short currencyDeposited = (short) (selling ? -getSellRate().getPrice() : getBuyRate().getPrice());
		final short materialDeposited = (short) (selling ? getSellRate().getAmount() : -getBuyRate().getAmount());
		if(currencyDeposited != 0) {
			plugin
				.getLogBlock()
				.queueChestAccess(
					player
						.getName(),
					chestLocation,
					54,
					(short) (
						selling
							? getSellCurrency()
							: getBuyCurrency()
						)
						.getMaterial()
						.getId(),
					currencyDeposited,
					(byte) 0
					);
		}
		if (materialDeposited != 0 ) {
			plugin
				.getLogBlock()
				.queueChestAccess(
					player
						.getName(),
					chestLocation,
					54,
					(short) getMaterial()
						.getMaterial()
						.getId(),
					materialDeposited,
					(byte) 0
					);
		}
	}
	/**
	 * performs sell operation for player
	 * @param player player to sell something to shop
	 * @param plugin The active PhysicalShop plugin
	 * @return true if successful
	 */
	protected boolean sell(final Player player, final PhysicalShop plugin) {
		if (!canSell()) {
			plugin.getLocale().sendMessage(player, NO_SELL);
			return false;
		}

		final Inventory inventory = player.getInventory();

		final int price = getSellRate().getPrice();
		final int amount = getSellRate().getAmount();

		try {
			InventoryHelpers.exchange(inventory, getSellCurrency().getStack(price), material.getStack(amount));
		} catch (final InvalidExchangeException e) {
			switch (e.getType()) {
			case ADD:
				plugin.getLocale().sendMessage(player, PLAYER_INVENTORY_FULL);
				break;
			case REMOVE:
				plugin.getLocale().sendMessage(player, NOT_ENOUGH_PLAYER_ITEMS, material.toString(plugin.getMaterialConfig()));
				break;
			}

			return false;
		}

		updateInventory(player); // player.updateInventory();

		plugin.getLocale().sendMessage(
			player,
			SELL,
			amount,
			material.toString(plugin.getMaterialConfig()),
			price,
			getSellCurrency().toString(plugin.getMaterialConfig())
			);

		queryLogBlock(player, true, plugin);
		return true;
	}
	/**
	 * Messages player p the rates for current Shop.
	 * @param p the player to message
	 * @param plugin The active PhysicalShop plugin
	 */
	public void status(final Player p, final PhysicalShop plugin) {
		if (canBuy() && (getShopItems() >= buyRate.getAmount())) {
			plugin.getLocale().sendMessage(
				p,
				BUY_RATE,
				buyRate.getAmount(),
				material.toString(plugin.getMaterialConfig()),
				buyRate.getPrice(),
				getBuyCurrency().toString(plugin.getMaterialConfig())
				);
		}

		if (canSell() && (getShopSellCapital() >= sellRate.getPrice())) {
			plugin.getLocale().sendMessage(
				p,
				SELL_RATE,
				sellRate.getAmount(),
				material.toString(plugin.getMaterialConfig()),
				sellRate.getPrice(),
				getSellCurrency().toString(plugin.getMaterialConfig())
				);
		}
	}
	private void triggerRedstone(final PhysicalShop plugin) {
		if(!plugin.getConfig().getBoolean(TRIGGER_REDSTONE)) return;
		final BlockFace face = ShopHelpers.getBack(sign);
		switch(face) {
			case NORTH:
			case SOUTH:
			case WEST:
			case EAST:
				break;
			default:
				return;
		}
		final Block signBlock = sign.getBlock().getRelative(face);
		final Block activatedBlock = signBlock.getRelative(face);
		final Material type = activatedBlock.getType();

		if(type != Material.LEVER && type != Material.STONE_BUTTON) return;
		if(((Attachable) activatedBlock.getState().getData()).getAttachedFace() != face.getOppositeFace()) return;
		if(!(activatedBlock.getChunk() instanceof CraftChunk)) return;

		final CraftChunk chunk = (CraftChunk) activatedBlock.getChunk();

		net.minecraft.server.v1_4_6.Block
			.byId[chunk.getHandle().world.getTypeId(
				activatedBlock.getX(),
				activatedBlock.getY(),
				activatedBlock.getZ()
				)
			].interact(
				chunk.getHandle().world,
				activatedBlock.getX(),
				activatedBlock.getY(),
				activatedBlock.getZ(),
				null,
				0,
				Float.NaN,
				Float.NaN,
				Float.NaN
				);
		// This is Notch code for toggling something.
		// This means I wont need to toggle the button back myself!
	}
}
