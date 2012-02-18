package net.betterverse.townsgates;

import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.TheDgtl.Stargate.Portal;
import net.TheDgtl.Stargate.event.StargateAccessEvent;
import net.TheDgtl.Stargate.event.StargateCreateEvent;
import net.TheDgtl.Stargate.event.StargateDestroyEvent;

import net.betterverse.towns.NotRegisteredException;
import net.betterverse.towns.object.Resident;
import net.betterverse.towns.object.Town;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class StargateListener implements Listener {
	private TownsGates sgt;
	
	public StargateListener(TownsGates sgt) {
		this.sgt = sgt;
	}
	
	@EventHandler
	public void onStargateCreate(StargateCreateEvent event) {
		Player player = event.getPlayer();
		Portal portal = event.getPortal();
		Block sign = portal.getSign();
		
		// Check if we're building a Town gate
		if (!event.getLine(1).equalsIgnoreCase("TOWN")) {
			return;
		}
		
		// Check if this build is in a town
		Town town = sgt.checkTown(portal.getEntrances(), sign.getWorld().getName());
		if (town == null) {
			return;
		}
		
		// Check if we have a resident.
		Resident res = sgt.getResident(player.getName());
		
		// Check if the resident is mayor or an assistant
		if (res == null || !town.isMayor(res) && !town.hasAssistant(res)) {
			return;
		}
		
		// Check if this gate layout is allowable as a Town gate
		boolean isAllowed = false;
		String pGate = portal.getGate().getFilename();
		for(String gate : sgt.getGateList()) {
			if ((gate + ".gate").equalsIgnoreCase(pGate)) {
				isAllowed = true;
				break;
			}
		}
		if (!isAllowed) {
			event.setDenyReason("That gate format is not allowed as a town gate");
			event.setDeny(true);
			return;
		}
		
		// Check if we meet the minimum resident requirement
		if (town.getResidents().size() < sgt.getMinResidents()) {
			event.setDenyReason("There are not enough residents in your town");
			event.setDeny(true);
			return;
		}
		
		int townId = sgt.getTownId(town.getName());
		String townNetwork = String.format("TOWN%04d", townId);
		
		ArrayList<String> network = Portal.getNetwork(townNetwork);
		if (network != null && network.size() >= sgt.getMaxGates()) {
			event.setDenyReason("Your town network is too large");
			event.setDeny(true);
			return;
		}
		
		// Set portal values
		event.setDeny(false);
		portal.setNetwork(townNetwork);
		portal.setDestination("");
		portal.setNoNetwork(true);
		
		sgt.addPortal(portal.getName(), portal.getNetwork(), town.getName());
	}
	
	@EventHandler
	public void onStargateDestroy(StargateDestroyEvent event) {
		Portal portal = event.getPortal();
		Player player = event.getPlayer();
		String name = portal.getName();
		String network = portal.getNetwork();
		
		// Check if this stargate is part of a town network
		String townName = sgt.getPortalTown(name, network);
		if (townName == null) return;
		Town town = sgt.getTown(townName);
		if (town == null) return;
		
		// Check if the player is a resident
		Resident res = sgt.getResident(player.getName());
		
		// Check if the resident is mayor or an assistant
		if (res == null || !town.isMayor(res) && !town.hasAssistant(res)) {
			return;
		}
		
		event.setDeny(false);
		sgt.removePortal(name, network);
	}
	
	@EventHandler
	public void onStargateAccess(StargateAccessEvent event) {
		Portal portal = event.getPortal();
		Player player = event.getPlayer();
		String name = portal.getName();
		String network = portal.getNetwork();
		
		// Check if this gate is part of a town network
		String townName = sgt.getPortalTown(name, network);
		if (townName == null) return;
		Town town = sgt.getTown(townName);
		if (town == null) return;
		// Check if the player is a resident
		Resident res = sgt.getResident(player.getName());
		
		if (res == null || !town.hasResident(res)) {
			event.setDeny(true);
			return;
		}
		
		event.setDeny(false);
	}
}