package com.wolvereness.physicalshop.showcase;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import net.minecraft.server.v1_4_5.EntityItem;
import net.minecraft.server.v1_4_5.NetServerHandler;
import net.minecraft.server.v1_4_5.Packet;
import net.minecraft.server.v1_4_5.Packet21PickupSpawn;
import net.minecraft.server.v1_4_5.Packet29DestroyEntity;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.NumberConversions;

import com.wolvereness.physicalshop.ShopMaterial;

/**
 * Licensed under GNU GPL v3
 * @author Wolfe
 */
public class PlayerHandler {
	/**
	 * The MC version that it was compiled against.
	 */
	public static final String MC_VERSION = "(MC: 1.4.5)";

	/**
	 * This creates a destroy packet for reuse (and also gets an entity id)
	 * @param plugin Plugin to use for logging purposes
	 * @return A generic object that will be casted back when needed, or null if error
	 */
	public static Object getDestroyPacket(final Plugin plugin) {
		try {
			return new Packet29DestroyEntity(new EntityItem(null).id);
		} catch (final Throwable t) {
			plugin.getLogger().log(Level.SEVERE, "Problem creating remove packet", t);
			return null;
		}
	}

	/**
	 * @param packet The destroy packet that was created before
	 * @return the fresh EntityId to use
	 */
	public static int getEntityId(final Object packet) {
		return ((Packet29DestroyEntity) packet).a[0];
	}

	private static void putData(final byte[] store, final int index, final byte b) {
		store[index] = b;
	}

	private static void putData(final byte[] store, final int index, final int i) {
		store[index] = (byte) ((i >>> 24) & 0xFF);
        store[index + 1] = (byte) ((i >>> 16) & 0xFF);
        store[index + 2] = (byte) ((i >>>  8) & 0xFF);
        store[index + 3] = (byte) ((i >>>  0) & 0xFF);
	}

	/* For later if needed
	private static void putData(final byte[] store, final int index, final long l) {
        store[index] = (byte) (l >>> 56);
        store[index + 1] = (byte) (l >>> 48);
        store[index + 2] = (byte) (l >>> 40);
        store[index + 3] = (byte) (l >>> 32);
        store[index + 4] = (byte) (l >>> 24);
        store[index + 5] = (byte) (l >>> 16);
        store[index + 6] = (byte) (l >>>  8);
        store[index + 7] = (byte) (l >>>  0);
	}
	//*/

	private static void putData(final byte[] store, final int index, final short s) {
        store[index] = (byte) ((s >>> 8) & 0xFF);
        store[index + 1] = (byte) ((s >>> 0) & 0xFF);
	}

	private final byte[] dataArray = new byte[26];
	private final DataInputStream inputStream = new DataInputStream(new InputStream() {
		private final byte[] dataArray = PlayerHandler.this.dataArray;
		@Override
		public int read() throws IOException {
			int m;
			byte[] d;
			if ((m = marker + 1) == (d = dataArray).length) {
				m = 0;
			}
			return d[marker = m] & 0xFF;
		}});
	private final Packet21PickupSpawn itemPacket = new Packet21PickupSpawn();
	private final ShowcaseListener listener;
	private int marker = -1;
	private final NetServerHandler netHandler;

	/**
	 * Creates a new PlayerHandler, that will reuse one packet for updating
	 * @param player the player for this handler
	 * @param showcaseListener The showcase listener using this handler
	 */
	public PlayerHandler(final Player player, final ShowcaseListener showcaseListener) {
		netHandler = ((CraftPlayer) player).getHandle().netServerHandler;
		listener = showcaseListener;

		putData(dataArray, 0, showcaseListener.getEntityId());
		putData(dataArray, 6, (byte) 16);
		putData(dataArray, 23, (byte) 0);
		putData(dataArray, 24, (byte) (0.2d * 128.0D));
		putData(dataArray, 25, (byte) 0);
	}

	/**
	 * This method is to indicate finalization of this handler
	 */
	public void close() {
		try {
			inputStream.close();
		} catch (final IOException e) {
		}
	}

	/**
	 * This method will queue the appropriate packets to the player
	 * @param loc Location to put the item
	 * @param item the shop item to display
	 * @param skipDestroy indicates if the destroy packet should not be sent
	 */
	public void handle(
						final Location loc,
						final ShopMaterial item,
						final boolean skipDestroy) {
		//*
		if (!skipDestroy) {
			netHandler.sendPacket((Packet) listener.getDestroyPacket());
		}
		//*/

		final byte[] dataArray = this.dataArray;
		putData(dataArray, 4, (short) item.getMaterial().getId());
		putData(dataArray, 7, item.getDurability());
		putData(dataArray, 9, (short) -1); // tag compound byte array size, -1 ignores the tag compound
		putData(dataArray, 11, NumberConversions.floor(loc.getX() * 32.0D));
		putData(dataArray, 15, NumberConversions.floor(loc.getY() * 32.0D));
		putData(dataArray, 19, NumberConversions.floor(loc.getZ() * 32.0D));

		itemPacket.a(inputStream); // This prevents variable references, hopefully maintaining forward compatibility
		if (marker != dataArray.length - 1)
			throw new IllegalStateException("Packet length changed! Expected:" + (dataArray.length - 1) + " got:" + marker);
		netHandler.sendPacket(itemPacket);
	}
}
