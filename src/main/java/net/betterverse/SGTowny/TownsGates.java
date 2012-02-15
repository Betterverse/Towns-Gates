package net.betterverse.SGTowny;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import net.TheDgtl.Stargate.Blox;

import net.betterverse.towns.NotRegisteredException;
import net.betterverse.towns.Towns;
import net.betterverse.towns.object.Coord;
import net.betterverse.towns.object.Town;
import net.betterverse.towns.object.TownsUniverse;
import net.betterverse.towns.object.TownsWorld;
import net.betterverse.towns.object.WorldCoord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class TownsGates extends JavaPlugin {
	private Logger log;
	private FileConfiguration config;
	private Towns towny;
	
	// Data Set
	private HashMap<String, Integer> towns = null;
	private HashMap<String, String> portals = null;
	private int maxTownId = -1;
	
	// Configuration Variables
	private int maxGates = 10;
	private int minResidents = 10;
	private List<String> gateList;

	public void onEnable() {
		log = getServer().getLogger();
		config = getConfig();
		loadConfig();
		loadDatabase();
		log.info("[Stargate-Towny] Enabled Stargate-Towny v" + getDescription().getVersion());
		
		// Register Listeners
		getServer().getPluginManager().registerEvents(new StargateListener(this), this);
		
		towny = (Towns)getServer().getPluginManager().getPlugin("Towny");
	}
	
	public void onDisable() {
		saveDatabase();
	}
	
	public void loadConfig() {
		this.reloadConfig();
		this.config = getConfig();
		config.options().copyDefaults(true);
		
		maxGates = config.getInt("maxGates");
		minResidents = config.getInt("minResidents");
		gateList = config.getStringList("gateList");
		
		this.saveConfig();
	}
	
	public void loadDatabase() {
		// Clear existing towns/portals
		maxTownId = -1;
		if (towns == null) {
			towns = new HashMap<String, Integer>();
		}
		towns.clear();
		
		if (portals == null) {
			portals = new HashMap<String, String>();
		}
		portals.clear();
		
		// Load the town database (id:town)
		File db = new File(this.getDataFolder(), "towns.db");
		if (db.exists()) {
			try {
				Scanner scanner = new Scanner(db);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine().trim();
					String[] split = line.split(":");
					if (split.length < 2) continue;
					Integer townId = Integer.valueOf(split[0]);
					towns.put(split[1], townId);
					if (townId > maxTownId) {
						maxTownId = townId;
					}
				}
				scanner.close();
			} catch (Exception e) {
				log.severe("[SGTowny] Could not load town database");
				e.printStackTrace();
			}
		}
		
		// Load the portal database (portal:network:town)
		db = new File(this.getDataFolder(), "portals.db");
		if (db.exists()) {
			try {
				Scanner scanner = new Scanner(db);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine().trim();
					String[] split = line.split(":");
					if (split.length < 3) continue;
					String portal = split[0];
					String network = split[1];
					String town = split[2];
					portals.put(portal + ":" + network, town);
				}
				scanner.close();
			} catch (Exception e) {
				log.severe("[SGTowny] Could not load portal database");
				e.printStackTrace();
			}
		}
	}
	
	public void saveDatabase() {
		// Save town DB
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this.getDataFolder(), "towns.db"), false));
			for(String town : towns.keySet()) {
				Integer townId = towns.get(town);
				bw.append(townId.toString());
				bw.append(":");
				bw.append(town);
				bw.newLine();
			}
			bw.close();
		} catch (Exception ex) {
			log.severe("[SGTowny] There was an error saving the town database.");
			ex.printStackTrace();
		}
		
		// Save portal DB
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this.getDataFolder(), "portals.db"), false));
			for(String portal : portals.keySet()) {
				String town = portals.get(portal);
				bw.append(portal);
				bw.append(":");
				bw.append(town);
				bw.newLine();
			}
			bw.close();
		} catch (Exception ex) {
			log.severe("[SGTowny] There was an error saving the portal database.");
			ex.printStackTrace();
		}
	}
	
	public Town checkTown(Blox[] blox, String world) {
		TownsWorld tw;
		Town town = null;
		try {
			tw = TownsUniverse.getWorld(world);
		} catch (NotRegisteredException ex) {
			return null;
		}
		
		for (Blox b : blox) {
			try {
				WorldCoord wc = new WorldCoord(tw, Coord.parseCoord(b.getBlock()));
				town = wc.getTownBlock().getTown();
				if (town != null) return town;
			} catch (NotRegisteredException ex) {
				continue;
			}
		}
		return null;
	}
	
	public Town getTown(String name) {
		try {
			return towny.getTownsUniverse().getTown(name);
		} catch (NotRegisteredException e) {
			return null;
		}
	}
	
	public void addPortal(String name, String network, String town) {
		portals.put(name + ":" + network, town);
		saveDatabase();
	}
	
	public void removePortal(String name, String network) {
		portals.remove(name + ":" + network);
	}
	
	public String getPortalTown(String name, String network) {
		return portals.get(name + ":" + network);
	}
	
	public int getTownId(String name) {
		Integer townId = towns.get(name);
		if (townId == null) {
			towns.put(name, ++maxTownId);
			saveDatabase();
			return maxTownId;
		}
		return townId;
	}
	
	// Getters
	public Towns getTowny() {
		return towny;
	}
	
	public int getMaxGates() {
		return maxGates;
	}
	
	public int getMinResidents() {
		return minResidents;
	}
	
	public List<String> getGateList() {
		return gateList;
	}
	
	// Command Handler
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("sgtowny")) {
			if (!sender.hasPermission("sgtowny.admin")) {
				sender.sendMessage("[SGTowny] Permission Denied");
				return true;
			}
			if (args.length != 1) return false;
			if (args[0].equalsIgnoreCase("reload")) {
				loadConfig();
				loadDatabase();
				sender.sendMessage("[SGTowny] Reloaded.");
				return true;
			}
		}
		return false;
	}
}
