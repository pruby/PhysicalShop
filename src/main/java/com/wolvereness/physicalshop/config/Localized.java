package com.wolvereness.physicalshop.config;

import static com.wolvereness.physicalshop.config.ConfigOptions.LANGUAGE;
import static java.util.logging.Level.SEVERE;
import static org.bukkit.ChatColor.translateAlternateColorCodes;
import static org.bukkit.configuration.file.YamlConfiguration.loadConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.google.common.io.ByteStreams;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class Localized {

	/**
	 * @author Wolfe
	 * Represents all the messages that can be sent
	 */
	@SuppressWarnings("javadoc")
	public enum Message {
		/**
		 * Amount<br>
		 * Material<br>
		 * Price<br>
		 * Currency<br>
		 */
		BUY,
		/**
		 * Amount<br>
		 * Material<br>
		 * Price<br>
		 * Currency
		 */
		BUY_RATE,
		CANT_BUILD,
		CANT_BUILD_SERVER,
		CANT_DESTROY,
		CANT_PLACE_CHEST,
		CANT_USE,
		CANT_USE_CHEST,
		CHEST_INVENTORY_FULL,
		EXISTING_CHEST,
		NO_BUY,
		NO_SELL,
		/**
		 * Material
		 */
		NOT_ENOUGH_PLAYER_ITEMS,
		/**
		 * Currency
		 */
		NOT_ENOUGH_PLAYER_MONEY,
		/**
		 * Item type
		 */
		NOT_ENOUGH_SHOP_ITEMS,
		/**
		 * Shop currency type
		 */
		NOT_ENOUGH_SHOP_MONEY,
		PLAYER_INVENTORY_FULL,
		/**
		 * Amount<br>
		 * Material<br>
		 * Price<br>
		 * Currency<br>
		 */
		SELL,
		SELL_RATE,
		/**
		 * Shop Buy currency amount<br>
		 * Shop Buy currency type<br>
		 * Shop Sell currency amount<br>
		 * Shop Sell currency type<br>
		 * Shop item amount<br>
		 * Shop item type
		 */
		STATUS,
		/**
		 * Shop currency amount<br>
		 * Shop currency type<br>
		 * Shop item amount<br>
		 * Shop item type
		 */
		STATUS_ONE_CURRENCY,
		/**
		 * Shop item amount<br>
		 * Shop item type
		 */
		STATUS_ONE_MATERIAL
	}

	/**
	 * Regex to find the & symbols to be replaced
	 * @deprecated Use {@link org.bukkit.ChatColor#translateAlternateColorCodes(char, String)}
	 */
	@Deprecated
	public static Pattern colorReplace = Pattern.compile("&(?=[0-9a-f])");
	private final YamlConfiguration config;
	private final Logger logger;
	private final Random random;

	/**
	 * @param plugin plugin to consider for getting resources
	 */
	public Localized(final Plugin plugin) {
		this(plugin, new Random());
	}

	/**
	 * @param plugin plugin to consider for getting resources
	 * @param random a random number generator, if messages have multiple outputs
	 */
	public Localized(final Plugin plugin, final Random random) {
		this.random = random;
		this.logger = plugin.getLogger();
		final String language = String.valueOf(plugin.getConfig().get(LANGUAGE)).toUpperCase();
		final File file = new File(plugin.getDataFolder(),"Locales" + File.separatorChar +  language + ".yml");
		if(file.exists()) {
			config = loadConfiguration(file);
		} else {
			config = new YamlConfiguration();
		}
		InputStream resource = plugin.getResource("Locales/" + language + ".yml");
		final YamlConfiguration defaults = new YamlConfiguration();
		if(resource == null) {
			resource = plugin.getResource("Locales/ENGLISH.yml");
		}
		try {
			defaults.loadFromString(new String(ByteStreams.toByteArray(resource), "UTF-8"));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} catch (final InvalidConfigurationException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				resource.close();
			} catch (final IOException e) {
			}
		}
		config.addDefaults(defaults);
		config.options().copyDefaults(true);
		try {
			config.save(file);
		} catch (final IOException e) {
			plugin.getLogger().log(Level.WARNING, "Failed to save locale file " + file, e);
		}
	}

	/**
	 * @param message message to get
	 * @return message stored
	 */
	public String getMessage(final Message message) {
		final String string = toFormattableMessage(message);
		return string == null ? null : translateAlternateColorCodes('&', string);
	}

	/**
	 * This recursion will only be a problem with nested values,
	 * where an infinite loop is worse than stack overflow.
	 */
	private String parseObject(final Object obj) {
		if (obj == null)
			return null;
		if (obj.getClass().isArray())
			return parseObject(Array.get(obj, (int) (random.nextDouble() * Array.getLength(obj))));
		if (obj instanceof List) {
			final List<?> list = (List<?>) obj;
			return parseObject(list.get((int) (random.nextDouble() * list.size())));
		}
		return obj.toString();
	}

	/**
	 * Sends the recipient a formatted message located at the node defined
	 * @param recipient Player to receive the message
	 * @param message Message to send
	 * @param args The arguments to String.format
	 */
	public void sendMessage(final CommandSender recipient, final Message message, final Object...args) {
		final String string = toFormattableMessage(message);
		if (string == null) {
			recipient.sendMessage("ERROR_"+message);
			logger.log(SEVERE,"Unknown message:" + message + " name:" + message.name(), new Exception());
		} else {
			recipient.sendMessage(translateAlternateColorCodes('&', String.format(string, args)));
		}
	}

	private String toFormattableMessage(final Message message) {
		Validate.notNull(message, "Cannot retrieve message for null");
		return parseObject(config.get(message.name()));
	}
}
