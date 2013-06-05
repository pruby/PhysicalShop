package com.wolvereness.physicalshop.events;

import java.util.Collection;
import java.util.Collections;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wolvereness.physicalshop.Shop;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class ShopDestructionEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
    /**
     * @return The HandlerList for this event
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private final Cancellable event;
    private final Entity player;
	private final Collection<Shop> shop;
    /**
     * @param event The event that caused this sign creation
     * @param shops The shops destroyed in this event
     * @param player The player that destroyed the shops, or null if not a player
     */
    public ShopDestructionEvent(final Cancellable event, final Collection<Shop> shops, final Entity player) {
        this.event = event;
        this.shop = shops;
        this.player = player;
    }
    /**
     * @return the Event that caused this event
     */
    public Cancellable getCause() {
        return event;
    }
    /**
     * @return the Entity that destroyed these shops, or null if physics
     */
    public Entity getEntity() {
    	return this.player;
    }
    @Override
	public HandlerList getHandlers() {
        return handlers;
    }
    /**
     * @return The shops that are destroyed in this event
     */
    public Collection<Shop> getShops() {
    	return Collections.unmodifiableCollection(shop);
    }
	public boolean isCancelled() {
		return event.isCancelled();
	}
	public void setCancelled(final boolean cancel) {
		event.setCancelled(true);
	}
}