package com.redstoner.parcels;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldInitEvent;

import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.WorldManager;

public class ParcelListener implements Listener {
	
	private static final WorldManager MANAGER = ParcelsPlugin.getInstance().getWorldManager();
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		
	}
	
	@EventHandler
	public void onWorldInit(WorldInitEvent event) {
		
	}
	
	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		Player user = event.getPlayer();
		if (!user.hasPermission("parcels.admin.buildanywhere")) {
			Optional<Parcel> maybe = MANAGER.getParcelAt(user.getLocation());
			if (!maybe.isPresent() || !maybe.get().canBuild(user))
				event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent event) {
		Player user = event.getPlayer();
		if (!user.hasPermission("parcels.admin.buildanywhere")) {
			Optional<Parcel> maybe = MANAGER.getParcelAt(user.getLocation());
			if (!maybe.isPresent() || !maybe.get().canBuild(user))
				event.setCancelled(true);
		}
	}

}
