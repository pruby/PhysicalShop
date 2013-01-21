package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.ShopHelpers.*;
import static com.wolvereness.physicalshop.config.ConfigOptions.SERVER_SHOP;
import static com.wolvereness.physicalshop.config.Localized.Message.*;
import static java.util.logging.Level.SEVERE;
import static org.bukkit.Material.CHEST;
import static org.bukkit.block.BlockFace.DOWN;
import static org.bukkit.block.BlockFace.UP;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import com.wolvereness.physicalshop.events.ShopCreationEvent;
import com.wolvereness.physicalshop.events.ShopDestructionEvent;
import com.wolvereness.physicalshop.events.ShopInteractEvent;
import com.wolvereness.physicalshop.events.ShopSignCreationEvent;
import com.wolvereness.physicalshop.exception.InvalidSignException;
import com.wolvereness.physicalshop.exception.InvalidSignOwnerException;
import com.wolvereness.util.NameCollection;
import com.wolvereness.util.NameCollection.OutOfEntriesException;

/**
 * Licensed under GNU GPL v3
 * @author Wolfe
 */
public class PhysicalShopListener implements Listener {
	private final PhysicalShop plugin;
	/**
	 * Default constructor
	 * @param plugin PhysicalShop plugin to consider
	 */
	public PhysicalShopListener(final PhysicalShop plugin) {
		this.plugin = plugin;
	}
	/**
	 * Central place for checking for an event cancellation
	 * @param e Event
	 * @param p Player
	 * @param b Block
	 * @return true if event is cancelled
	 */
	public boolean onBlock_Destroyed(final Cancellable e, final Entity p, final Block b) {
		return onBlock_Destroyed(e, p, Collections.singleton(b));
	}
	/**
	 * Central place for checking for an event cancellation
	 * @param e Event
	 * @param entity Player or Entity causing the event
	 * @param blocks a Collection of blocks to check
	 * @return true if event is cancelled
	 */
	public boolean onBlock_Destroyed(final Cancellable e, final Entity entity, final Collection<Block> blocks) {
		final Collection<Shop> shops = getShops(blocks, plugin, new HashSet<Shop>());
		final Player p = entity instanceof Player ? (Player) entity : null;
		if (plugin.getPluginConfig().isProtectBreak() && !isShopsDestroyable(shops, p , plugin)) {
			if(p != null) {
				plugin.getLocale().sendMessage(p, CANT_DESTROY);
			}
			e.setCancelled(true);
			return true;
		}
		if (!shops.isEmpty()) {
			plugin.getServer().getPluginManager().callEvent(new ShopDestructionEvent(e, shops, entity));
		}
		return e.isCancelled();
	}
	/**
	 * Block Break event
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBlockBreak(final BlockBreakEvent e) {
		onBlock_Destroyed(e, e.getPlayer(), e.getBlock());
	}
	/**
	 * Block BlockBurnEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBlockBurn(final BlockBurnEvent e) {
		onBlock_Destroyed(e, null, e.getBlock());
	}
	/**
	 * Block LeavesDecayEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBlockFade(final BlockFadeEvent e) {
		onBlock_Destroyed(e, null, e.getBlock());
	}
	/**
	 * Block BlockPhysicsEvent if it destroyed shop
	 * @deprecated Doesn't quite work as intended
	 * @param e Event
	 */
	//@EventHandler(ignoreCancelled = true)
	@Deprecated
	public void onBlockBlockPhysics(final BlockPhysicsEvent e) {
		onBlock_Destroyed(e, null, e.getBlock());
	}
	/**
	 * Block the BlockPistonExtendEvent if it will move the block
	 * a store sign is on.
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBlockPistonExtend(final BlockPistonExtendEvent e) {
		onBlock_Destroyed(e, null, e.getBlocks());
	}
	/**
	 * Block the BlockPistonRetractEvent if it will move the block
	 * a store sign is on.
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBlockPistonRetract(final BlockPistonRetractEvent e) {
		final BlockFace direction = e.getDirection();

		final ArrayList<Block> blocks = new ArrayList<Block>(2);
		// We need to check to see if a sign is attached to the piston piece
		final Block b = e.getBlock();
		blocks.add(b.getRelative(direction));
		if(!e.isSticky()) { // We only care about the second block if sticky piston is retracting.
			blocks.add(b.getRelative(direction, 2));
		}

		onBlock_Destroyed(e, null, blocks);
	}
	/**
	 * Stop EntityChangeBlockEvents from affecting stores.
	 * Prevents Endermen from breaking stores, etc.
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockEntityChangeBlock(final EntityChangeBlockEvent e) {
		onBlock_Destroyed(e, e.getEntity(), e.getBlock());
	}
	/**
	 * Entity Explode event
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockEntityExplode(final EntityExplodeEvent e) {
		onBlock_Destroyed(e, e.getEntity(), e.blockList());
	}
	/**
	 * Block LeavesDecayEvent if it destroyed shop
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockLeavesDecay(final LeavesDecayEvent e) {
		onBlock_Destroyed(e, null, e.getBlock());
	}
	/**
	 * Block Place event
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(final BlockPlaceEvent e) {
		final Block block;
		final BlockState state;
		if (	!plugin.getPluginConfig().isProtectChestAccess()
				|| plugin.getPermissionHandler().hasAdmin(e.getPlayer())
				|| !((state = (block = e.getBlock()).getState()) instanceof InventoryHolder)
				) return;

		if (	block.getType() == CHEST
				? isProtectedChestsAround(block, e.getPlayer(), plugin)
				: !hasAccess(e.getPlayer(), block.getRelative(UP), plugin)
				) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_PLACE_CHEST);
			e.setCancelled(true);
			return;
		}

		final Shop placedShop = getShop(state, plugin);
		if(placedShop != null) {
			plugin.getServer().getPluginManager().callEvent(new ShopCreationEvent(e, placedShop));
		}
	}
	/**
	 * This listens for new shop creation and checks to see if the player has permission to build over a chest
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onNewShopSign(final ShopSignCreationEvent e) {
		if (!e.isCheckExistingChest()) return;
		final Block b = e.getCause().getBlock().getRelative(DOWN);
		if (	plugin.lwcCheck(b, e.getCause().getPlayer())
				|| plugin.locketteCheck(b, e.getCause().getPlayer())
				|| plugin.deadboltCheck(b, e.getCause().getPlayer())) {
			e.setCheckExistingChest(false);
		}
	}
	/**
	 * Player Interact event
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEvent e) {
		final Block block = e.getClickedBlock();
		if (	plugin.getPluginConfig().isProtectChestAccess()
				&& e.getAction() == RIGHT_CLICK_BLOCK
				&& block.getState() instanceof InventoryHolder
				&& !plugin.getPermissionHandler().hasAdmin(e.getPlayer())
				) {
			if (	block.getType() == CHEST
					? isProtectedChestsAround(block, e.getPlayer(), plugin)
					: !hasAccess(e.getPlayer(), block.getRelative(UP), plugin)
					) {
				plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE_CHEST);
				e.setCancelled(true);
				return;
			}
			return;
		}

		final Shop shop = getShop(block, plugin);

		if (shop == null) return;

		if (!plugin.getPermissionHandler().hasUse(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE);
			return;
		}

		plugin.getServer().getPluginManager().callEvent(new ShopInteractEvent(e, shop));
	}
	/**
	 * Shop Interact event
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onShopInteract(final ShopInteractEvent e) {
		if (e.getAction() == LEFT_CLICK_BLOCK) {
			e.getShop().status(e.getPlayer(), plugin);
		} else if (e.getAction() == RIGHT_CLICK_BLOCK) {
			e.getShop().interact(e.getPlayer(), plugin);
			e.setCancelled(true);
		}
	}
	/**
	 * Sign Change event
	 * @param e Event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(final SignChangeEvent e) {
		try {
			new Shop(e.getLines(), plugin);
		} catch (final InvalidSignOwnerException ex) {
		} catch (final InvalidSignException ex) {
			return;
		}

		if (!plugin.getPermissionHandler().hasBuild(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_BUILD);
			e.setCancelled(true);
			return;
		}

		final ShopSignCreationEvent event = new ShopSignCreationEvent(e);
		if (e.getLine(3).equalsIgnoreCase(plugin.getConfig().getString(SERVER_SHOP))) {
			if (!plugin.getPermissionHandler().hasAdmin(e.getPlayer())) {
				plugin.getLocale().sendMessage(e.getPlayer(), CANT_BUILD_SERVER);
				e.setCancelled(true);
			}
			plugin.getServer().getPluginManager().callEvent(event.setCheckExistingChest(false));
		} else {
			if (plugin.getPluginConfig().isAutoFillName()) {
				if(plugin.getPluginConfig().isExtendedNames()) {
					try {
						e.setLine(3, NameCollection.getSignName(e.getPlayer().getName()));
					} catch (final OutOfEntriesException ex) {
						plugin.getLogger().severe("Player " + e.getPlayer() + " cannot register extended name!");
						e.getPlayer().sendMessage("Name overflow, notify server administrator!");
						e.setCancelled(true);
						return;
					}
				} else {
					e.setLine(3, truncateName(e.getPlayer().getName()));
				}
			}
			plugin.getServer().getPluginManager().callEvent(event.setCheckExistingChest(plugin.getPluginConfig().isExistingChestProtected()));
		}
		if (e.isCancelled()) return;
		final boolean hasChest;
		if (event.isCheckExistingChest()) {
			final BlockState state = e.getBlock().getRelative(DOWN).getState();
			if (	state instanceof InventoryHolder
					&& !plugin.getPluginConfig().isBlacklistedShopType(state.getType())) {
				if (!plugin.getPermissionHandler().hasAdmin(e.getPlayer())) {
					plugin.getLocale().sendMessage(e.getPlayer(), EXISTING_CHEST);
					e.setCancelled(true);
					return;
				}
				hasChest = true;
			} else {
				hasChest = false;
			}
		} else {
			hasChest = e.getBlock().getRelative(DOWN).getState() instanceof InventoryHolder;
		}
		if(hasChest || e.getLine(3).equalsIgnoreCase(plugin.getConfig().getString(SERVER_SHOP))) {
			try {
				plugin.getServer().getPluginManager().callEvent(new ShopCreationEvent(e, new Shop(e.getLines(), plugin)));
			} catch (final InvalidSignException ex) {
				plugin.getLogger().log(SEVERE, "Unexpected invalid shop", ex);
			}
		}
	}
}
