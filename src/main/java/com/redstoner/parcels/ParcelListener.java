package com.redstoner.parcels;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldInitEvent;

import com.redstoner.parcels.api.WorldManager;

public class ParcelListener implements Listener {
	
	private static final WorldManager MANAGER = ParcelsPlugin.getInstance().getWorldManager();
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		/*
		IntStream.range(0, 19).forEach(i -> {
			event.getPlayer().sendMessage(" ");
		});
		MANAGER.getParcelAt(event.getTo()).ifPresent(parcel -> {
			event.getPlayer().sendMessage("You are on " + parcel.toString());
		});
		*/
	}
	
	@EventHandler
	public void onWorldInit(WorldInitEvent event) {
		
	}
	
	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		Player user = event.getPlayer();
		if (user.hasPermission("parcels.admin.buildanywhere"))
			return;
		Block b = event.getBlock();
		MANAGER.getWorld(b.getWorld().getName()).ifPresent(w -> {
			w.getParcelAt(b.getX(), b.getZ())
			.ifNotPresent(() -> event.setCancelled(true))
			.filter(p -> !p.canBuild(user) || !w.isInParcel(b.getX(), b.getZ(), p.getX(), p.getZ()))
			.ifPresent(p -> event.setCancelled(true));
		});		
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent event) {
		Player user = event.getPlayer();
		if (user.hasPermission("parcels.admin.buildanywhere"))
			return;
		Block b = event.getBlockPlaced();
		MANAGER.getWorld(b.getWorld().getName()).ifPresent(w -> {
			w.getParcelAt(b.getX(), b.getZ())
			.ifNotPresent(() -> event.setCancelled(true))
			.filter(p -> !p.canBuild(user) || !w.isInParcel(b.getX(), b.getZ(), p.getX(), p.getZ()))
			.ifPresent(p -> event.setCancelled(true));
		});		
	}

}
