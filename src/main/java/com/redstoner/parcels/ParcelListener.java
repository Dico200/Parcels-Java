package com.redstoner.parcels;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldInitEvent;

import com.redstoner.parcels.api.WorldManager;

public class ParcelListener implements Listener {
	
	private static void checkBuildEvent(Cancellable event, Block b, Player user) {
		if (user.hasPermission("parcels.admin.buildanywhere"))
			return;
		WorldManager.ifWorldPresent(b, (w, maybeP) -> {
			if (!maybeP.isPresent() || maybeP.map(p -> !p.canBuild(user) || !w.isInParcel(b.getX(), b.getZ(), p.getX(), p.getZ())).orElse(false))
				cancel(event);
		});	
	}
	
	private static void cancel(Cancellable event) {
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		/*
		if (event.getPlayer().hasPermission("parcels.admin.bypass"))
			return;
		WorldManager.getParcel(event.getTo()).filter(p -> p.getDenied().contains(event.getPlayer())).ifPresent(() -> cancel(event));
		//TODO NOT WORKING
		 */
	}
	
	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		checkBuildEvent(event, event.getBlock(), event.getPlayer());
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent event) {
		checkBuildEvent(event, event.getBlockPlaced(), event.getPlayer());
	}
	
	@EventHandler
	public void onWorldInit(WorldInitEvent event) {
		WorldManager.getWorld(event.getWorld()).ifPresent(w -> {
			event.getWorld().getGenerator(); //TODO
		});
	}

}
