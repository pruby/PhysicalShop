package com.wolvereness.physicalshop.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.wolvereness.physicalshop.Shop;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
@SuppressWarnings("javadoc")
public class ShopInteractEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
    /**
     * @return The HandlerList for this event
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
	private final PlayerInteractEvent event;
	private final Shop shop;
    /**
     * @param event The event that caused this sign creation
     * @param shops The shops destroyed in this event
     * @param player The player that destroyed the shops, or null if not a player
     */
    public ShopInteractEvent(final PlayerInteractEvent event, final Shop shop) {
        this.event = event;
        this.shop = shop;
    }
    /**
	 * @see org.bukkit.event.player.PlayerInteractEvent#getAction()
	 */
	public Action getAction() {
		return event.getAction();
	}
    /**
	 * @see org.bukkit.event.player.PlayerInteractEvent#getBlockFace()
	 */
	public BlockFace getBlockFace() {
		return event.getBlockFace();
	}
    /**
     * @return The PlayerInteractEvent that caused this event
     */
    public PlayerInteractEvent getCause() {
    	return event;
    }
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#getClickedBlock()
	 */
	public Block getClickedBlock() {
		return event.getClickedBlock();
	}
	@Override
	public HandlerList getHandlers() {
        return handlers;
    }
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#getItem()
	 */
	public ItemStack getItem() {
		return event.getItem();
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#getMaterial()
	 */
	public Material getMaterial() {
		return event.getMaterial();
	}
	/**
	 * @see org.bukkit.event.player.PlayerEvent#getPlayer()
	 */
	public final Player getPlayer() {
		return event.getPlayer();
	}
	/**
     * @return The shop that is created from this event
     */
    public Shop getShop() {
    	return shop;
    }
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#hasBlock()
	 */
	public boolean hasBlock() {
		return event.hasBlock();
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#hasItem()
	 */
	public boolean hasItem() {
		return event.hasItem();
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#isBlockInHand()
	 */
	public boolean isBlockInHand() {
		return event.isBlockInHand();
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#isCancelled()
	 */
	public boolean isCancelled() {
		return event.isCancelled();
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#setCancelled(boolean)
	 */
	public void setCancelled(final boolean cancel) {
		event.setCancelled(cancel);
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#setUseInteractedBlock(org.bukkit.event.Event.Result)
	 */
	public void setUseInteractedBlock(final Result useInteractedBlock) {
		event.setUseInteractedBlock(useInteractedBlock);
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#setUseItemInHand(org.bukkit.event.Event.Result)
	 */
	public void setUseItemInHand(final Result useItemInHand) {
		event.setUseItemInHand(useItemInHand);
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#useInteractedBlock()
	 */
	public Result useInteractedBlock() {
		return event.useInteractedBlock();
	}
	/**
	 * @see org.bukkit.event.player.PlayerInteractEvent#useItemInHand()
	 */
	public Result useItemInHand() {
		return event.useItemInHand();
	}
}