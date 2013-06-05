package com.wolvereness.physicalshop.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.SignChangeEvent;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class ShopSignCreationEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
    /**
     * @return The HandlerList for this event
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private boolean checkExistingChest;
    private final SignChangeEvent event;
    /**
     * @param event The event that caused this sign creation
     */
    public ShopSignCreationEvent(final SignChangeEvent event) {
        this.event = event;
    }
    /**
     * @return the SignChangeEvent that caused this event
     */
    public SignChangeEvent getCause() {
        return event;
    }
    @Override
	public HandlerList getHandlers() {
        return handlers;
    }
	public boolean isCancelled() {
		return event.isCancelled();
	}
	/**
	 * @return The current status
	 */
	public boolean isCheckExistingChest() {
		return checkExistingChest;
	}
	public void setCancelled(final boolean cancel) {
		event.setCancelled(true);
	}
	/**
	 * @param status new status of the chest existing chest flag
	 * @return this
	 */
	public ShopSignCreationEvent setCheckExistingChest(final boolean status) {
		checkExistingChest = status;
		return this;
	}
}