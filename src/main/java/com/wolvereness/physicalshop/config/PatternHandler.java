package com.wolvereness.physicalshop.config;

import static com.wolvereness.physicalshop.config.ConfigOptions.*;
import static java.util.logging.Level.SEVERE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;

import com.wolvereness.physicalshop.PhysicalShop;
import com.wolvereness.physicalshop.Rate;
import com.wolvereness.physicalshop.ShopMaterial;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class PatternHandler {
	/**
	 * @author Wolfe
	 * Licensed under GNU GPL v3
	 */
	public enum PatternType {
		/**
		 * This type of pattern will use matchers to find elements.
		 */
		MATCH,
		/**
		 * This type of pattern will split to find elements.
		 */
		SPLIT;
	}
	private final int amountIndex;
	private final int currencyIndex;
	private final Pattern pattern;
	private final int priceIndex;
	private final PatternType type;
	/**
	 * @param config The section to build this handler from
	 */
	public PatternHandler(final ConfigurationSection config) {
		type = PatternType.valueOf(config.getString(MODE));
		if(type == null) throw new RuntimeException(config.getString(MODE) + " is not a valid PatternType");
		amountIndex = config.getInt(AMOUNT_INDEX);
		currencyIndex = config.getInt(CURRENCY_INDEX);
		priceIndex = config.getInt(PRICE_INDEX);
		pattern = Pattern.compile(config.getString(PATTERN));
	}
	/**
	 * @param line The line from the shop sign
	 * @param plugin The currently active PhysicalShop plugin
	 * @return a rate to use, or null if not found.
	 */
	public Rate getRate(final String line, final PhysicalShop plugin) {
		final int amount, price;
		final ShopMaterial material;
		switch(type) {
		case MATCH:
			final Matcher matcher = pattern.matcher(line);
			if(!matcher.matches()) return null;
			try {
				amount = Integer.parseInt(matcher.group(amountIndex));
				price = Integer.parseInt(matcher.group(priceIndex));
				final String currency = matcher.group(currencyIndex);
				if(currency == null || currency.length() == 0) return null;
				material = plugin.getMaterialConfig().getCurrency(currency);
			} catch (final IndexOutOfBoundsException e) {
				plugin.getLogger().log(SEVERE, "There is an issue with the regex '" + pattern + '\'', e);
				return null;
			} catch (final NumberFormatException e) {
				return null;
			}
			break;
		case SPLIT:
			final String[] splitLine = pattern.split(line);
			if(		splitLine.length <= amountIndex
					|| splitLine.length <= priceIndex
					|| splitLine.length <= currencyIndex) return null;
			try {
				amount = Integer.parseInt(splitLine[amountIndex]);
				price = Integer.parseInt(splitLine[priceIndex]);
				material = plugin.getMaterialConfig().getCurrency(splitLine[currencyIndex]);
			} catch (final NumberFormatException e) {
				return null;
			}
			break;
		default:
			throw new UnsupportedOperationException("PatternHandler type not supported");
		}
		if(material == null) return null;
		return new Rate(amount, price, material);
	}
}
