package com.wolvereness.physicalshop.showcase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.ImmutableMap;
import com.wolvereness.physicalshop.ShopMaterial;

/**
 * Licensed under GNU GPL v3
 * @author Wolfe
 */
public class PlayerHandler {

	static abstract class Invoker {

		Object call() {
			return this.<RuntimeException>invoke(null, (Object[]) null);
		}

		Object call(final Object on) {
			return this.<RuntimeException>invoke(on, (Object[]) null);
		}

		Object call(final Object on, final Object...params) {
			return this.<RuntimeException>invoke(on, params);
		}

		abstract <T extends Throwable> Object invoke(Object on, Object...params) throws T;
	}

	private static final Map<String, Invoker> constructors;
	private static final String DATA;
	private static final String DESTROY;
	private static final String ENTITY;
	private static final Map<Class<?>, Invoker> grabbers = new HashMap<Class<?>, Invoker>();
	private static final String SPAWN;

	private static final String VELOCITY;
	static {
		ENTITY = "Entity";
		DESTROY = "Destroy";
		SPAWN = "Spawn";
		DATA = "Data";
		VELOCITY = "Velocity";
	}


	static {
		final String craftBukkit = "org.bukkit.craftbukkit.";
		final String nms = "net.minecraft.server.";
		final String classPackage = ClassUtils.getPackageName(Bukkit.getServer().getClass());
		if (!classPackage.startsWith(craftBukkit))
			throw new AssertionError("Unknown implementation " + Bukkit.getVersion() + " with server " + Bukkit.getServer().getClass().getName());

		final String version = classPackage.substring(craftBukkit.length());
		final String nmsPath = nms.concat(version);
		final String craftBukkitPath = craftBukkit.concat(version);

		try {
			constructors = ImmutableMap.of(
				ENTITY,
					new Invoker() {
						final Field[] mot = new Field[2];
						final Field motY;
						final Constructor<?> newEntity;
						final Object[] params;

						{
							final Class<?> world      = Class.forName(nmsPath.concat(".World"));
							final Class<?> entity     = Class.forName(nmsPath.concat(".Entity"));
							final Class<?> itemStack  = Class.forName(nmsPath.concat(".ItemStack"));
							final Class<?> entityItem = Class.forName(nmsPath.concat(".EntityItem"));

							final Object dummyStack = itemStack
								.getConstructor( int.class, int.class, int.class)
								.newInstance   (         1,         1,         0);

							newEntity = entityItem.getConstructor( world, double.class, double.class, double.class,  itemStack );
							params    = new Object[]             {  null,         0.0d,         0.0d,         0.0d, dummyStack };

							mot[0] = entity.getField("motX");
							mot[1] = entity.getField("motZ");
							motY   = entity.getField("motY");
						}

						@Override
						public <T extends Throwable> Object invoke(final Object on, final Object...params) throws T {
							try {
								final Object entity = newEntity.newInstance(this.params);

								for (final Field mot : this.mot) {
									mot.setDouble(entity, 0d);
								}
								motY.setDouble(entity, 0.20000000298023224D / 4);

								return entity;
							} catch (final InvocationTargetException ex) {
								throw (T) ex.getCause();
							} catch (final Throwable t) {
								throw (T) t;
							}
						}
					},
				DESTROY,
					new Invoker() {
						final Field id;
						final Constructor<?> newDestroy;

						{
							final Class<?> destroyEntity = Class.forName(nmsPath.concat(".Packet29DestroyEntity"));
							final Class<?> entity        = Class.forName(nmsPath.concat(".Entity"));

							newDestroy = destroyEntity.getConstructor(int[].class);
							id = entity.getField("id");
						}

						@Override
						public <T extends Throwable> Object invoke(final Object entity, final Object...params) throws T {
							try {
								return newDestroy.newInstance(new int[] { (Integer) id.get(entity) });
							} catch (final InvocationTargetException ex) {
								throw (T) ex.getCause();
							} catch (final Throwable t) {
								throw (T) t;
							}
						}
					},
				VELOCITY,
					new Invoker() {
						final Constructor<?> newVelocity;

						{
							final Class<?> velocity = Class.forName(nmsPath.concat(".Packet28EntityVelocity"));
							final Class<?> entity    = Class.forName(nmsPath.concat(".Entity"));

							newVelocity = velocity.getConstructor(entity);
						}

						@Override
						public <T extends Throwable> Object invoke(final Object entity, final Object...params) throws T {
							try {
								return newVelocity.newInstance(entity);
							} catch (final InvocationTargetException ex) {
								throw (T) ex.getCause();
							} catch (final Throwable t) {
								throw (T) t;
							}
						}
					},
				SPAWN,
					new Invoker() {
						final Constructor<?> newSpawn;
						final Method setLocation;

						{
							final Class<?> entity = Class.forName(nmsPath.concat(".Entity"));
							final Class<?> spawn  = Class.forName(nmsPath.concat(".Packet23VehicleSpawn"));

							newSpawn = spawn.getConstructor(entity, int.class);
							setLocation = entity.getMethod("setPosition", double.class, double.class, double.class);
						}

						@Override
						public <T extends Throwable> Object invoke(final Object entity, final Object...params) throws T {
							try {
								final Location location = (Location) params[0];
								setLocation.invoke(entity, location.getX(), location.getY(), location.getZ());
								return newSpawn.newInstance(entity, 2);
							} catch (final InvocationTargetException ex) {
								throw (T) ex.getCause();
							} catch (final Throwable t) {
								throw (T) t;
							}
						}
					},
				DATA,
					new Invoker() {
						final Method getMeta;
						final Field id;
						final Constructor<?> newData;
						final Method newNMSStack;
						final Method setItem;

						{
							final Class<?> entity = Class.forName(nmsPath.concat(".Entity"));
							final Class<?> item   = Class.forName(nmsPath.concat(".EntityItem"));
							final Class<?> data   = Class.forName(nmsPath.concat(".Packet40EntityMetadata"));

							id = entity.getField("id");
							getMeta = entity.getMethod("getDataWatcher", (Class[]) null);
							newData = data.getConstructor(int.class, getMeta.getReturnType(), boolean.class);

							final Class<?> craftStack = Class.forName(craftBukkitPath.concat(".inventory.CraftItemStack"));

							newNMSStack = craftStack.getMethod("asNMSCopy", ItemStack.class);
							setItem = item.getMethod("setItemStack", newNMSStack.getReturnType());
						}

						@Override
						public <T extends Throwable> Object invoke(final Object entity, final Object...params) throws T {
							// [0] = NMS.EntityItem
							// [1] = OB.Inventory.ItemStack
							try {
								// Switch bukkit for nms in cached array
								params[0] = newNMSStack.invoke(null, params[0]);
								setItem.invoke(entity, params);

								final Object id = this.id.get(entity);
								final Object meta = getMeta.invoke(entity, (Object[]) null);

								return newData.newInstance(id, meta, true);
							} catch (final InvocationTargetException ex) {
								throw (T) ex.getCause();
							} catch (final Throwable t) {
								throw (T) t;
							}
						}
					}
				);
		} catch (final RuntimeException ex) {
			throw ex;
		} catch (final Error ex) {
			throw ex;
		} catch (final Throwable ex) {
			throw new AssertionError(ex);
		}
	}

	/*
	 * Packet 29 - destroy
	 * Packet 23 - spawn
	 * Packet 40 - data
	 * Packet 28 - velocity
	 */

	// Static methods to avoid the anonymous references to the enclosing class in the anonymous objects stored in static context.

	private static Invoker getHandler(final Class<?> clazz) {
		try {
			final Method getHandle = clazz.getMethod("getHandle", (Class[]) null);
			return
				new Invoker() {
					@Override
					public <T extends Throwable> Object invoke(final Object on, final Object...params) throws T {
						final Object handle;
						try {
							handle = getHandle.invoke(on, params);
						} catch (final InvocationTargetException ex) {
							throw (T) ex.getCause();
						} catch (final Throwable t) {
							throw (T) t;
						}

						Invoker getSubHandler = grabbers.get(handle.getClass());
						if (getSubHandler == null) {
							grabbers.put(handle.getClass(), getSubHandler = getSubHandler(handle.getClass()));
						}
						return getSubHandler.call(handle);
					}
				};
		} catch (final NoSuchMethodException ex) {
			throw new IllegalArgumentException(clazz + " does not have a getHandle() method", ex);
		}
	}

	private static Invoker getSubHandler(final Class<?> clazz) {
		try {
			final Field field = clazz.getField("playerConnection");
			return
				new Invoker() {
					@Override
					public <T extends Throwable> Object invoke(final Object on, final Object...params) throws T {
						try {
							return field.get(on);
						} catch (final Throwable t) {
							throw (T) t;
						}
					}
				};
		} catch (final NoSuchFieldException e) {
			throw new IllegalArgumentException(clazz + " does not have a field \"playerConnection\"", e);
		}
	}

	private static Invoker sendPacket(final Class<?> clazz) {
		for (final Method sendPacket : clazz.getMethods()) {
			if (!"sendPacket".equals(sendPacket.getName())) {
				continue;
			}
			return
				new Invoker() {
					@Override
					public <T extends Throwable> Object invoke(final Object on, final Object...params) throws T {
						try {
							return sendPacket.invoke(on, params);
						} catch (final InvocationTargetException ex) {
							throw (T) ex.getCause();
						} catch (final Throwable t) {
							throw (T) t;
						}
					}
				};
		}
		throw new IllegalArgumentException(clazz + " does not have sendPacket method");
	}

	private final Object[] destroyPacket;
	private final Object entity;
	private final Object netHandler;
	private final Invoker sendPacket;
	private final Object[] velocity;

	/**
	 * Creates a new PlayerHandler, that will reuse one packet for updating
	 * @param player the player for this handler
	 */
	public PlayerHandler(final Player player) {
		Invoker getHandler = grabbers.get(player.getClass());
		if (getHandler == null) {
			grabbers.put(player.getClass(), getHandler = getHandler(player.getClass()));
		}
		netHandler = getHandler.call(player);

		if (!grabbers.containsKey(netHandler.getClass())) {
			grabbers.put(netHandler.getClass(), sendPacket = sendPacket(netHandler.getClass()));
		} else {
			sendPacket = grabbers.get(netHandler.getClass());
		}

		this.entity = constructors.get(ENTITY).call();
		this.destroyPacket = new Object[] { constructors.get(DESTROY).call(entity) };
		this.velocity = new Object[] { constructors.get(VELOCITY).call(entity) };
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
		if (!skipDestroy) {
			sendPacket.call(netHandler, destroyPacket);
		}

		sendPacket.call(netHandler, constructors.get(SPAWN).call(entity, loc));
		sendPacket.call(netHandler, constructors.get(DATA).call(entity, item.getStack(16)));
		sendPacket.call(netHandler, velocity);
	}
}
