package com.wolvereness.physicalshop.config;

import static com.wolvereness.physicalshop.config.ConfigOptions.CURRENCIES;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.wolvereness.physicalshop.PhysicalShop;
import com.wolvereness.physicalshop.ShopMaterial;
import com.wolvereness.physicalshop.exception.InvalidMaterialException;

/**
 * @author Wolfe
 *
 */
public class MaterialConfig {
	private static final Pattern spaces = Pattern.compile("\\s+");
	private final FileConfiguration config;
	private final HashMap<String, ShopMaterial> currencies = new HashMap<String, ShopMaterial>();
	private final File file;
	private final HashMap<String, ShopMaterial> identifiers = new HashMap<String, ShopMaterial>();
	private final Pattern junkCharacters = Pattern.compile("[^A-Za-z0-9:_]");
	private final HashMap<ShopMaterial, String> names = new HashMap<ShopMaterial, String>();
	private final PhysicalShop plugin;
	/**
	 * Creates a MaterialConfiguration
	 * @param plugin Plugin to use
	 */
	public MaterialConfig(final PhysicalShop plugin) {
		this.plugin = plugin;
		final ConfigurationSection currencySection = plugin.getConfig().getConfigurationSection(CURRENCIES);
		for(final String currency : currencySection.getKeys(false)) {
			addCurrency(currency, currencySection.getString(currency));
		}
		config = YamlConfiguration.loadConfiguration(file = new File(plugin.getDataFolder(), "Locales" + File.separatorChar +  "Items.yml"));
		defaults();
		try {
			config.save(file);
		} catch (final IOException e) {
			plugin.getLogger().log(Level.WARNING, "Failed to save material configuration", e);
		}
	}
	/**
	 * Adds currency represented by item.
	 * @param currencyIdentifier character to use as a reference
	 * @param item name of the item to reference
	 */
	private void addCurrency(final String currencyIdentifier, final String item) {
		try {
			currencies.put(currencyIdentifier, new ShopMaterial(junkCharacters.matcher(spaces.matcher(item).replaceAll("_")).replaceAll("").toUpperCase()));
		} catch (final InvalidMaterialException e) {
			plugin.getLogger().severe("Configuration error for shop currency:'"+currencyIdentifier+"' for item:"+item);
		}
	}
	/**
	 * Adds an alias to use to reference a shop material
	 * @param alias string to use as reference
	 * @param item name of the item to reference
	 */
	public void addShopMaterialAlias(String alias, final String item) {
		alias = junkCharacters.matcher(spaces.matcher(alias).replaceAll("_")).replaceAll("").toUpperCase();
		try {
			identifiers.put(alias,getShopMaterial(item, false));
		} catch (final InvalidMaterialException e) {
			plugin.getLogger().log(Level.WARNING, "Configuration error for material alias: "+alias+" mapping to: "+item, e);
		}
	}
	private String checkPattern(final String string) throws InvalidMaterialException {
		final Matcher m = plugin.getPluginConfig().getMaterialPattern().matcher(string);

		if (!m.find()) throw new InvalidMaterialException();
		return m.group(1);
	}
	private void defaults() {
		if(!config.isConfigurationSection("Aliases")) {
			config.set("Aliases.custom_name", "real item name or number");
		}
		final ConfigurationSection aliasSection = config.getConfigurationSection("Aliases");
		final Set<String> aliases = aliasSection.getKeys(false);
		if (!(aliases.size() == 1 && "real item name or number".equals(aliasSection.get("custom_name")))) {
			for(final String alias : aliases) {
				addShopMaterialAlias(alias, String.valueOf(aliasSection.get(alias)).replace('|', ':'));
			}
		}

		if(!config.isConfigurationSection("Names")) {
			config.set("Names.real_item_name_or_number|damage_value", "custom item name");
		}
		final ConfigurationSection nameSection = config.getConfigurationSection("Names");
		final Set<String> names = nameSection.getKeys(false);
		if(!(names.size() == 1 && "custom item name".equals(nameSection.get("real_item_name_or_number|damage_value")))) {
			for(final String name : names) {
				setMaterialName(name.replace('|', ':'),String.valueOf(nameSection.get(name)));
			}
		}
	}
	/**
	 * Searches for ShopMaterial associated with currency character.
	 * @param currencyIdentifier The character the shop will be associated with.
	 * @return ShopMaterial Associated with the currencyIdentifier, or null if not found
	 */
	public ShopMaterial getCurrency(final String currencyIdentifier) {
		if (currencyIdentifier.equals("*")) {
			try {
				return new ShopMaterial("*");
			} catch (InvalidMaterialException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return currencies.get(currencyIdentifier);
	}
	/**
	 * Retrieves the material based on a name.
	 * @param name Name to search / interpret
	 * @return ShopMaterial that should be associated with the name.
	 * @throws InvalidMaterialException is name is invalid
	 */
	public ShopMaterial getShopMaterial(final String name) throws InvalidMaterialException {
		return getShopMaterial(name,true);
	}
	/**
	 * Retrieves the material based on a name.
	 * @param name Name to search / interpret
	 * @param checkPattern Checks to see if name matches pattern
	 * @return ShopMaterial that should be associated with the name.
	 * @throws InvalidMaterialException if name is invalid
	 */
	public ShopMaterial getShopMaterial(String name, final boolean checkPattern) throws InvalidMaterialException {
		name = junkCharacters.matcher(spaces.matcher(checkPattern ? checkPattern(name) : name).replaceAll("_")).replaceAll("").toUpperCase();
		if(identifiers.containsKey(name)) return identifiers.get(name);
		return new ShopMaterial(name);
	}

	/**
	 * @param shopMaterial shop material to check
	 * @return true if it is configured for custom output, false otherwise
	 */
	public boolean isConfigured(final ShopMaterial shopMaterial) {
		return names.containsKey(shopMaterial);
	}
	/**
	 * Sets the name that a shop material should display
	 * @param material material name to reference
	 * @param name the name to give the material
	 */
	private void setMaterialName(final String material, final String name) {
		try {
			names.put(getShopMaterial(material, false), name);
		} catch (final InvalidMaterialException e) {
			plugin.getLogger().warning("Configuration error for material name: "+name+" mapping from: "+material);
		}
	}
	/**
	 * @param shopMaterial shop material to make into string
	 * @return the string representation of the shop material
	 */
	public String toString(final ShopMaterial shopMaterial) {
		return names.get(shopMaterial);
	}
	/**
	 * Prints a large amount of output for debugging purposes
	 * @param sender The person to send the output to
	 */
	public void verbose(final CommandSender sender) {
		final StringBuilder builder = new StringBuilder();
		for(final Map.Entry<String, ShopMaterial> currency : currencies.entrySet()) {
			currency.getValue().toStringDefault(builder.append(currency.getKey()).append(" represents ")).append('\n');
		}
		for(final Entry<String, ShopMaterial> identifier : identifiers.entrySet()) {
			identifier.getValue().toStringDefault(builder.append(identifier.getKey()).append(" can be used for ")).append('\n');
		}
		for(final Entry<ShopMaterial, String> name : names.entrySet()) {
			name.getKey().toStringDefault(builder).append(" is printed as ").append(name.getValue()).append('\n');
		}
		sender.sendMessage(builder.toString());
	}
}
