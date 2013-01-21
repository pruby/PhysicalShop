package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.ConfigOptions.SERVER_SHOP;
import static org.bukkit.Material.*;
import static org.bukkit.block.BlockFace.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import com.google.common.collect.ImmutableList;
import com.wolvereness.physicalshop.exception.InvalidSignException;

/**
 *
 */
public class ShopHelpers {
	/**
	 * A list of block faces, North East South West, and self
	 */
	public static final List<BlockFace> CARDINAL_DIRECTIONS = ImmutableList.of(
			SELF,
			NORTH,
			SOUTH,
			EAST,
			WEST);
	/**
	 * A list of block faces including the cardinal directions and up, down
	 */
	private static final List<BlockFace> EXTENDED_DIRECTIONS = ImmutableList.of(
			SELF,
			NORTH,
			SOUTH,
			EAST,
			WEST,
			DOWN,
			UP);

	/**
	 * @param sign Sign to check
	 * @return The assumed direction behind the sign
	 */
	public static BlockFace getBack(final Sign sign) {
		if (sign.getType() == SIGN_POST) {
			switch (((org.bukkit.material.Sign) sign.getData()).getFacing()) {

			case SOUTH_EAST:
			case SOUTH_SOUTH_EAST:
			case EAST_SOUTH_EAST:
				return NORTH_WEST;

			case NORTH_WEST:
			case NORTH_NORTH_WEST:
			case WEST_NORTH_WEST:
				return SOUTH_EAST;

			case SOUTH_WEST:
			case SOUTH_SOUTH_WEST:
			case WEST_SOUTH_WEST:
				return NORTH_EAST;

			case NORTH_EAST:
			case NORTH_NORTH_EAST:
			case EAST_NORTH_EAST:
				return SOUTH_WEST;

			case SOUTH:
				return NORTH;
			case EAST:
				return WEST;
			case WEST:
				return EAST;
			case NORTH:
				return SOUTH;

			default:
				throw new AssertionError(((org.bukkit.material.Sign) sign.getData()).getFacing());
			}
		}
		return ((org.bukkit.material.Sign) sign.getData()).getAttachedFace();
	}

	/**
	 * Attempts to create a new shop object based on this block
	 * @param block the block to consider
	 * @param plugin The active PhysicalShop plugin
	 * @return null if block is not sign or said sign is invalid, otherwise a new associated {@link Shop} for this block
	 */
	public static Shop getShop(final Block block, final PhysicalShop plugin) {
		if (block == null) return null;

		if (block.getType() != SIGN_POST && block.getType() != WALL_SIGN) return null;

		final Sign sign = (Sign) block.getState();
		if (sign == null) return null;

		final String ownerName = Shop.getOwnerName(sign.getLines());

		try {
			final BlockState state = block.getRelative(DOWN).getState();
			if (	state instanceof InventoryHolder
					&& !plugin.getPluginConfig().isBlacklistedShopType(state.getType())
					) return new ChestShop(sign, plugin, (InventoryHolder) state);
			else if (ownerName.equalsIgnoreCase(plugin.getConfig().getString(SERVER_SHOP))) return new Shop(sign, plugin);
			else return null;
		} catch (final InvalidSignException e) {
			return null;
		}
	}
	/**
	 * Attempts to create a chest shop based on the blockstate (being an {@link org.bukkit.inventory.InventoryHolder})
	 * @param chest The blockstate of an inventory holder
	 * @param plugin The currently active PhysicalShop plugin
	 * @return null
	 *  if the given chest block state is not an inventory holder,
	 *  if the sign above it does not exist,
	 *  if the sign above it is not valid,
	 *  otherwise a newly created ChestShop
	 */
	public static ChestShop getShop(final BlockState chest, final PhysicalShop plugin) {
		if (chest == null || !(chest instanceof InventoryHolder)) return null;

		final Block signBlock = chest.getBlock().getRelative(UP);
		if (signBlock.getType() != SIGN_POST && signBlock.getType() != WALL_SIGN) return null;

		final Sign sign = (Sign) signBlock.getState();
		if (sign == null) return null;

		try {
			return new ChestShop(sign, plugin, (InventoryHolder) chest);
		} catch (final InvalidSignException e) {
			return null;
		}
	}
	/**
	 * Adds the shops associated with the specified block to the provided collection
	 * @param block The block to check around
	 * @param plugin The currently active PhysicalShop plugin
	 * @param shops The collection to add to
	 * @return the provided collection
	 */
	public static Collection<Shop> getShops(final Block block, final PhysicalShop plugin, final Collection<Shop> shops) {
		Validate.notNull(shops, "Must provide a collection to add result to");
		for (final BlockFace face : EXTENDED_DIRECTIONS) {
			final Shop shop = getShop(block.getRelative(face), plugin);

			if (shop != null && shop.isShopBlock(block)) {
				shops.add(shop);
			}
		}
		return shops;
	}
	/**
	 * Adds the shops associated with the specified blocks to the provided collection
	 * @param blocks The blocks to check around
	 * @param plugin The currently active PhysicalShop plugin
	 * @param shops The set to add to
	 * @return the provided collection
	 */
	public static Collection<Shop> getShops(final Collection<Block> blocks, final PhysicalShop plugin, final Set<Shop> shops) {
		for (final Block block : blocks) {
			getShops(block, plugin, shops);
		}
		return shops;
	}
	/**
	 * This assumes player does NOT have admin access
	 * @param player Player to check for access
	 * @param block Block to check for access
	 * @param plugin The current instance of PhysicalShop
	 * @return true if the player has permission for the shop
	 */
	public static boolean hasAccess(final Player player, final Block block, final PhysicalShop plugin) {
		return hasAccess(player.getName(), getShop(block, plugin), plugin);
	}
	/**
	 * This assumes player does NOT have admin access
	 * @param player Player to check for access
	 * @param shop Shop to check for access
	 * @param plugin The current instance of PhysicalShop
	 * @return true if the player has permission for the shop
	 */
	public static boolean hasAccess(final String player, final Shop shop, final PhysicalShop plugin) {
		return shop == null || (
			!plugin.getConfig().getString(SERVER_SHOP).equals(shop.getOwnerName())
			&& shop.isSmartOwner(player, plugin)
			);
	}
    /**
	 * This method checks a block for shop protection for other chests near or that chest<br>
	 * This will ONLY check for chests!
	 * @param block Block to chest, intended to be a chest
	 * @param player Player to cross-check for permissions
	 * @param plugin currently active PhysicalShop to consider
	 * @return true if the player should be blocked
	 */
	public static boolean isProtectedChestsAround(final Block block, final Player player, final PhysicalShop plugin) {
		for (final BlockFace blockFace : CARDINAL_DIRECTIONS) {
			final Block checkBlock = block.getRelative(blockFace);
			if(checkBlock.getType() == CHEST && !hasAccess(player, checkBlock.getRelative(UP), plugin)) return true;
		}
		return false;
	}
	/**
	 * Checks a list of shops against player for ownership.<br>
	 * Assumes block ARE protected.
	 * @param shops The shops being destroyed
	 * @param player The player destroying block, can be null (as in, no destroyer)
	 * @param plugin The active PhysicalShop plugin
	 * @return false if there are shops and player is null or not admin and not owner
	 */
	public static boolean isShopsDestroyable(
			final Collection<Shop> shops,
			final Player player,
			final PhysicalShop plugin) {
		if(shops.isEmpty()) return true;
		if(player == null) return false;
		if(plugin.getPermissionHandler().hasAdmin(player)) return true;
		for (final Shop shop : shops) {
			if (!hasAccess(player.getName(), shop, plugin)) {
				shop.getSign().update();
				return false;
			}
		}
		return true;
	}
	/**
	 * Cuts the name to 15 characters
	 * @param name name to truncate
	 * @return the first 15 characters of the name
	 */
	public static String truncateName(final String name) {
		if(name == null) return null;
		if(name.length()<=15) return name;
		return name.substring(0, 15);
	}
}
