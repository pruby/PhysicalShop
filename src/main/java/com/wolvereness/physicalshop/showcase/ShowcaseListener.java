package com.wolvereness.physicalshop.showcase;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerQuitEvent;

import com.wolvereness.physicalshop.PhysicalShop;
import com.wolvereness.physicalshop.Shop;
import com.wolvereness.physicalshop.ShopMaterial;
import com.wolvereness.physicalshop.events.ShopInteractEvent;

/**
 * Licensed under GNU GPL v3
 * @author Wolfe
 */
public class ShowcaseListener implements Listener {
	/**
	 * The MC version that it was compiled against.
	 */
	public static final String MC_VERSION = "(MC: 1.4.6)";

	private final Object destroyPacket = null;
	private int entityId;
	private final Map<String, PlayerHandler> handlers = new HashMap<String, PlayerHandler>();
	private boolean listening = false;
	private final PhysicalShop plugin;
	private boolean status = false;

	/**
	 * Constructor to initialize the referenced plugin
	 * @param plugin the current instance of PhysicalShop
	 */
	public ShowcaseListener(final PhysicalShop plugin) {
		this.plugin = plugin;
	}

	/**
	 * This returns the reusable destroy packet that was created when enabled.
	 * @return the reusable destroyPacket
	 */
	public Object getDestroyPacket() {
		return destroyPacket;
	}

	/**
	 * This returns the currently assigned entityId for packets.
	 * @return the entity id to be used
	 */
	public int getEntityId() {
		return entityId;
	}

	/**
	 * The plugin for this listener
	 * @return the current PhysicalShop instance
	 */
	public PhysicalShop getPlugin() {
		return plugin;
	}

	/**
	 * This indicates if the listener is active.
	 * @return the current state
	 */
	public boolean isActive() {
		return status;
	}

	/**
	 * This method clears the stored packet and data streams
	 * @param event The quit event
	 */
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {
		if (!status)
			return;

		final PlayerHandler handler = handlers.remove(event.getPlayer().getName());
		if (handler == null)
			return;
	}

	/**
	 * This method handles interactions
	 * @param event The interact event
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onShopInteract(final ShopInteractEvent event) {
		if (!status || event.getAction() != Action.LEFT_CLICK_BLOCK)
			return;

		final Shop shop = event.getShop();
		final Location loc = shop.getSign().getLocation().add(.5d, 0d, .5d);
		final ShopMaterial item = shop.getMaterial();
		final Player player = event.getPlayer();
		PlayerHandler handler = handlers.get(player.getName());
		boolean skipDestroy;
		if (skipDestroy = (handler == null)) {
			handlers.put(player.getName(), handler = new PlayerHandler(player));
		}
		handler.handle(loc, item, skipDestroy);
	}

	/**
	 * This method will activate the listener if applicable
	 * @param showcaseEnabled new state for the listener
	 */
	public void setStatus(final boolean showcaseEnabled) {
		if (status == showcaseEnabled) return;
		if (status = showcaseEnabled) {
			if (!listening) {
				plugin.getServer().getPluginManager().registerEvents(this, plugin);
				plugin.getLogger().info("Showcase listener active");
				listening = true;
			}
		} else {
			handlers.clear();
		}
	}
}
