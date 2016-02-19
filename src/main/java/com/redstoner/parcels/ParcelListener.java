package com.redstoner.parcels;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.redstoner.command.Messaging;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.Formatting;
import com.redstoner.utils.Optional;

public class ParcelListener implements Listener {
	
	public static void register() {
		Bukkit.getPluginManager().registerEvents(new ParcelListener(), ParcelsPlugin.getInstance());
	}
	
	private static void checkBuildEvent(Cancellable event, Block b, Player user) {
		if (user.hasPermission("parcels.admin.buildanywhere"))
			return;
		WorldManager.ifWorldPresent(b, (w, maybeP) -> {
			if (!maybeP.filter(p -> p.canBuild(user) && w.isInParcel(b.getX(), b.getZ(), p.getX(), p.getZ())).isPresent())
				cancel(event);
		});	
	}
	
	private static void checkPistonAction(BlockPistonEvent event, List<Block> affectedBlocks) {
		Optional<ParcelWorld> mWorld = WorldManager.getWorld(event.getBlock());
		if (!mWorld.isPresent())
			return;
		
		ParcelWorld world = mWorld.get();
		for (Block block : affectedBlocks) {
			if (!world.getParcelAt(block.getX(), block.getZ()).isPresent()) {
				cancel(event);
				return;
			}
		}
		
		BlockFace direction = event.getDirection();
		Block other;
		for (Block block : affectedBlocks) {
			other = block.getRelative(direction);
			if (!affectedBlocks.contains(other) && !world.getParcelAt(other.getX(), other.getZ()).isPresent()) {
				cancel(event);
				return;
			}
		}
	}
	
	private static void cancel(Cancellable event) {
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMove(PlayerMoveEvent event) {
		Player user = event.getPlayer();
		if (user.hasPermission("parcels.admin.bypass"))
			return;
		Location to = event.getTo();
		WorldManager.getWorld(to.getWorld()).ifPresent(world -> {
			world.getParcelAt(to.getBlockX(), to.getBlockZ()).filter(parcel -> parcel.getDenied().contains(user)).ifPresent(() -> {
				Location from = event.getFrom();
				world.getParcelAt(from.getBlockX(), from.getBlockZ()).ifPresentOrElse(parcel -> {
					world.teleport(user, parcel);
					Messaging.send(user, "Parcels", Formatting.YELLOW, "You are denied from this parcel");
				}, () -> {
					event.setTo(event.getFrom());
				});
			});
		});
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
	public void onPistonExtend(BlockPistonExtendEvent event) {
		checkPistonAction(event, event.getBlocks());
	}
	
	@EventHandler
	public void onPistonRetract(BlockPistonRetractEvent event) {
		checkPistonAction(event, event.getBlocks());
	}
	
	@EventHandler
	public void onEntityExplosion(EntityExplodeEvent event) {
		WorldManager.getWorld(event.getLocation().getWorld()).filter(w -> w.getSettings().disableExplosions).ifPresent(() -> cancel(event));
	}

}
