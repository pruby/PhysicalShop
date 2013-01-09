package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.ConfigOptions.*;
import static java.util.logging.Level.SEVERE;
import static org.bukkit.permissions.PermissionDefault.OP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yi.acru.bukkit.Lockette.Lockette;

import com.daemitus.deadbolt.Deadbolt;
import com.daemitus.deadbolt.DeadboltPlugin;
import com.daemitus.deadbolt.Deadbolted;
import com.griefcraft.lwc.LWCPlugin;
import com.untamedears.citadel.Citadel;
import com.wolvereness.physicalshop.config.Localized;
import com.wolvereness.physicalshop.config.MaterialConfig;
import com.wolvereness.physicalshop.config.StandardConfig;
import com.wolvereness.physicalshop.showcase.ShowcaseListener;
import com.wolvereness.util.CommandHandler;
import com.wolvereness.util.CommandHandler.Reload;
import com.wolvereness.util.CommandHandler.ShortCommand;
import com.wolvereness.util.CommandHandler.Verbose;
import com.wolvereness.util.CommandHandler.Verbose.Verbosable;
import com.wolvereness.util.CommandHandler.Version;
import com.wolvereness.util.NameCollection;

import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;

/**
 *
 */
public class PhysicalShop extends JavaPlugin implements Verbosable {
	/**
	 * Command to reload PhysicalShop
	 */
	public static final String RELOAD_COMMAND = "RELOAD";
	/**
	 * Command to update
	 */
	private static final String UPDATE_COMMAND = "UPDATE";
	/**
	 * Command to print verbose
	 */
	public static final String VERBOSE_COMMAND = "VERBOSE";
	/**
	 * Command to get version
	 */
	public static final String VERSION_COMMAND = "VERSION";
	private final HashMap<String,CommandHandler> commands = new HashMap<String,CommandHandler>();
	private StandardConfig configuration;
	private Consumer consumer = null;
	private Plugin deadbolt = null;
	private final PhysicalShopListener listener = new PhysicalShopListener(this);
	private Localized locale;
	private Plugin lockette = null;
	private LWCPlugin lwc = null;
	private Citadel citadel = null;
	private MaterialConfig materialConfig;
	private Permissions permissions;
	private final ShowcaseListener showcaseListener = new ShowcaseListener(this);
	private final Set<String> updateSenders = new HashSet<String>();
	/**
	 * This function checks for Deadbolt, thus letting player create shop over
	 * existing chest
	 *
	 * @param block Block representing chest
	 * @param player Player creating sign
	 * @return Returns true if and only if Deadbolt is enabled AND block protected AND player is owner of chest
	 */
	public boolean deadboltCheck(final Block block, final Player player) {
		if (deadbolt == null || !deadbolt.isEnabled()) return false;
		final Deadbolted lock = Deadbolt.get(block);
		return lock.isProtected() && lock.isOwner(player);
	}
	private File getFileDestination(final CommandSender sender) {
		if(!getFile().getName().equals("PhysicalShop.jar")) {
			sender.sendMessage("The jar is not named PhysicalShop.jar, and will be renamed with .old extension");
			final File newOldfile = new File(getFile().getParent(), getFile().getName() + ".old");
			if(!getFile().renameTo(newOldfile)) {
				sender.sendMessage("Could not rename file!");
				return null;
			}
			return new File(getFile().getParent(), "PhysicalShop.jar");
		}
		final File updateFolder = getServer().getUpdateFolderFile();
		if(!updateFolder.exists()) {
			if(!updateFolder.mkdir()) {
				sender.sendMessage("Failed to create the update directory!");
			}
		} else if (!updateFolder.isDirectory()) {
			sender.sendMessage("Update folder is not a directory!");
		}
		return new File(getServer().getUpdateFolderFile(), "PhysicalShop.jar");
	}
	/**
	 * @return the locale
	 */
	public Localized getLocale() {
		return locale;
	}
	/**
	 * This function checks for LogBlock if not already found after plugin
	 * enabled
	 *
	 * @return LogBlock consumer
	 */
	public Consumer getLogBlock() {
		return consumer;
	}
	/**
	 * @return the MaterialConfig being used
	 */
	public MaterialConfig getMaterialConfig() {
		return materialConfig;
	}
	public Permissions getPermissionHandler() {
		return permissions;
	}
	/**
	 * Grabs the current StandardConfig being used.
	 *
	 * @return the configuration being used
	 */
	public StandardConfig getPluginConfig() {
		return configuration;
	}
	/**
	 * Method used to hook into lockette
	 * @param relative the block to consider
	 * @param player player to consider
	 * @return true if and only if lockette is enabled and player owns said block
	 */
	public boolean locketteCheck(final Block relative, final Player player) {
		if (lockette == null || !lockette.isEnabled()) return false;
		return Lockette.isOwner(relative, player.getName());
	}
	/**
	 * This function checks for LWC, thus letting player create shop over
	 * existing chest
	 *
	 * @param block
	 *            Block representing chest
	 * @param player
	 *            Player creating sign
	 * @return Returns true if LWC enabled and protection exists and player is admin of chest
	 */
	public boolean lwcCheck(final Block block, final Player player) {
		if (lwc == null || !lwc.isEnabled()) return false;
		return lwc.getLWC().canAdminProtection(player, block);
	}
	/**
	 * Method used to hook into Citadel
	 * @param block 
	 * 				block that might be locked
	 * @param name 
	 *				player name on the sign
	 * @return returns true if citadel is enabled and the player is allowed to access the block
	 */
	public boolean citadelCheck(final Block block, final String name) {
		if (citadel == null || !citadel.isEnabled()) return false;
		return citadel.playerCanAccessBlock(block, name);
	}
	/**
	 * This will capture the only command, /physicalshop. It will send version information to the sender, and it checks permissions and reloads config if there is proper permission to.
	 * @param sender Player / Console sending command
	 * @param command ignored
	 * @param label ignored
	 * @param args ignored
	 * @return true, errors are handled manually.
	 */
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		final String subCommand = args.length == 0 ? VERSION_COMMAND : args[0].toUpperCase();
		if(commands.containsKey(subCommand))
			return commands.get(subCommand).onCommand(sender, args, getPermissionHandler());
		return false;
	}
	/**
	 * Does nothing as of yet.
	 */
	@Override
	public void onDisable() {
		if(configuration.isExtendedNames()) {
			NameCollection.unregisterPlugin(this);
		}
	}
	/**
	 * Initialization routine
	 */
	@Override
	public void onEnable() {
		try
		{
			saveConfig();
			permissions = new Permissions(this);
			//Events
			final PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(listener, this);
			//Commands
			commands.put(RELOAD_COMMAND, new Reload(this));
			commands.put(VERSION_COMMAND, new Version(this,"%2$s version %1$s by Wolvereness, original by yli"));
			commands.put(VERBOSE_COMMAND, new Verbose(this));
			commands.put(UPDATE_COMMAND, new ShortCommand(this.permissions, "update", OP) {
				@Override
				public boolean go(final CommandSender sender) {
					update(sender);
					return true;
				}});

			//Hooks
			try {
				final Plugin temp = getServer().getPluginManager().getPlugin("LWC");
				if(temp instanceof LWCPlugin) {
					lwc = (LWCPlugin) temp;
				}
			} catch (final Throwable er) {
			}

			try {
				final Plugin temp = getServer().getPluginManager().getPlugin("Lockette");
				if(temp instanceof Lockette) {
					lockette = temp;
				}
			} catch (final Throwable er) {
			}

			try {
				final Plugin temp = getServer().getPluginManager().getPlugin("Deadbolt");
				if(temp instanceof DeadboltPlugin) {
					deadbolt = temp;
				}
			} catch (final Throwable er) {
			}
			
			try {
				final Plugin temp = getServer().getPluginManager().getPlugin("Citadel");
				if (temp instanceof Citadel) {
					citadel = (Citadel) temp;
				}
			} catch (final Throwable er) {
			}

			getLogger().info(getDescription().getFullName() + " enabled.");
		} catch (final Throwable t) {
			getLogger().log(SEVERE, getDescription().getFullName() + " failed to enable", t);
			getServer().getPluginManager().disablePlugin(this);
		}
	}
	@Override
	public void onLoad() {
	}
	@Override
	@SuppressWarnings("deprecation")
	public void reloadConfig() {
		if(configuration != null && configuration.isExtendedNames()) {
			NameCollection.unregisterPlugin(this);
		}
		super.reloadConfig();
		final FileConfiguration getConfig = getConfig();
		getConfig.options().copyDefaults(true);
		if(getConfig.isSet(BUY_PATTERN)) {
			getConfig.getConfigurationSection(BUY_SECTION).set(PATTERN, getConfig.getString(BUY_PATTERN));
			getConfig.set(BUY_PATTERN, null);
		}
		if(getConfig.isSet(SELL_PATTERN)) {
			getConfig.getConfigurationSection(SELL_SECTION).set(PATTERN, getConfig.getString(SELL_PATTERN));
			getConfig.set(SELL_PATTERN, null);
		}
		configuration = new StandardConfig(this);
		if(configuration.isExtendedNames()) {
			NameCollection.registerPlugin(this);
		}
		showcaseListener.setStatus(configuration.isShowcaseEnabled());
		locale = new Localized(this);
		materialConfig = new MaterialConfig(this);
		try {
			if (getConfig.getBoolean(LOG_BLOCK)) {
				final Plugin logblockPlugin = getServer().getPluginManager().getPlugin("LogBlock");
				if (logblockPlugin == null || !(logblockPlugin instanceof LogBlock)) {
					getLogger().warning("Failed to find LogBlock");
					consumer = null;
				} else {
					consumer = ((LogBlock) logblockPlugin).getConsumer();
					if (consumer == null) {
						getLogger().warning("Error getting LogBlock consumer");
					} else {
						getLogger().info("Sucessfully hooked into LogBlock");
					}
				}
			} else {
				consumer = null;
				getLogger().info("Did not hook into LogBlock");
			}
		} catch (final Throwable t) {
			consumer = null;
			getLogger().log(Level.SEVERE, "Error handling LogBlock", t);
		}
	}
	/**
	 * @param sender The player to confirm the update
	 */
	public void update(final CommandSender sender) {
		if(!updateSenders.contains(sender.getName())) {
			sender.sendMessage("This feature is experimental, type command again to confirm");
			updateSenders.add(sender.getName());
			return;
		}
		final File destination = getFileDestination(sender);
		final String updateURL = getConfig().getString(UPDATE_URL);
		if(destination == null) return;
		getServer().getScheduler().runTaskAsynchronously(this, new Runnable(){
			public void run() {
				InputStream in = null;
				FileOutputStream out = null;
				try {
					in = new URL(updateURL).openStream();
					out = new FileOutputStream(destination);
					for(int inByte = in.read(); inByte != -1; inByte = in.read()) {
						out.write(inByte);
					}
					out.flush();
					getServer().getScheduler().scheduleSyncDelayedTask(PhysicalShop.this, new Runnable() {
						public void run() {
							sender.sendMessage("Download complete. Next server restart PhysicalShop will be updated");
						}});
				} catch (final Throwable ex) {
					getLogger().log(SEVERE, sender.getName() + " initiated an update, but an issue has occured!", ex);
					getServer().getScheduler().scheduleSyncDelayedTask(PhysicalShop.this, new Runnable() {
						public void run() {
							sender.sendMessage("An issue occured, please check server logs for more information!");
						}});
				} finally {
					if(in != null) {
						try	{
							in.close();
						} catch (final IOException e) {}
					}
					if(out != null) {
						try	{
							out.close();
						} catch (final IOException e) {}
					}
				}
			}});
		sender.sendMessage("Update has started.");
	}
	public void verbose(final CommandSender sender) {
		materialConfig.verbose(sender);
	}
}
