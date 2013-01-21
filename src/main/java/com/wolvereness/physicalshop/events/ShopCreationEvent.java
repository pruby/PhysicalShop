package com.wolvereness.physicalshop.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wolvereness.physicalshop.Shop;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class ShopCreationEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
    /**
     * @return The HandlerList for this event
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private final Cancellable event;
    private final Shop shop;
    /**
     * @param event The event that caused this sign creation
     * @param placedShop The shop that is created from this event
     */
    public ShopCreationEvent(final Cancellable event, final Shop placedShop) {
        this.event = event;
        this.shop = placedShop;
    }
    /**
     * @return the BlockPlaceEvent or SignChangeEvent that caused this event
     */
    public Cancellable getCause() {
        return event;
    }
    @Override
	public HandlerList getHandlers() {
        return handlers;
    }
    /**
     * @return The shop that is created from this event
     */
    public Shop getShop() {
    	return shop;
    }
	public boolean isCancelled() {
		return event.isCancelled();
	}
	public void setCancelled(final boolean cancel) {
		event.setCancelled(true);
	}
}