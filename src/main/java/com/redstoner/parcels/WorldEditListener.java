package com.redstoner.parcels;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.redstoner.command.Messaging;
import com.redstoner.parcels.api.Permissions;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.Formatting;
import com.redstoner.utils.OneTimeRunner;
import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class WorldEditListener {
	
	public static void register(Plugin worldEdit) {
		if (worldEdit instanceof WorldEditPlugin) {
			((WorldEditPlugin) worldEdit).getWorldEdit().getEventBus().register(new WorldEditListener());
		}
	}
	
	@Subscribe(priority = Priority.VERY_EARLY)
	public void onEditSession(EditSessionEvent event) {
		
		WorldManager.getWorld(event.getWorld().getName()).ifPresent(world -> {
			
			Stage stage = event.getStage();
			if (stage != Stage.BEFORE_CHANGE && stage != Stage.BEFORE_HISTORY) {
				return;
			}
			
			Actor actor = event.getActor();
			if (actor == null || !actor.isPlayer()) {
				return;
			}
			
			Player user = Bukkit.getPlayer(actor.getUniqueId());
			if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
				return;
			}

			event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
				
				private OneTimeRunner messageSender = new OneTimeRunner(() -> {
					Messaging.send(user, "Parcels", Formatting.YELLOW, "You can't use WorldEdit there");
				});
				
				private boolean canBuild(int x, int z) {
					if (world.getParcelAt(x, z).filter(p -> p.canBuild(user)).isPresent()) 
						return true;
					messageSender.run();
					return false;
				}
				
				@Override
				public boolean setBlock(Vector coord, BaseBlock block) throws WorldEditException {
					return canBuild(coord.getBlockX(), coord.getBlockZ()) && super.setBlock(coord, block);
				}
				
				@Override
				public boolean setBiome(Vector2D coord, BaseBiome biome) {
					return canBuild(coord.getBlockX(), coord.getBlockZ()) && super.setBiome(coord, biome);
				}
				
			});
			
		});
	}
}
