package com.redstoner.parcels;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.InventoryHolder;

import com.redstoner.command.Messaging;
import com.redstoner.parcels.api.Parcel;
import com.redstoner.parcels.api.ParcelWorld;
import com.redstoner.parcels.api.Permissions;
import com.redstoner.parcels.api.WorldManager;
import com.redstoner.utils.Formatting;
import com.redstoner.utils.Optional;

public class ParcelListener implements Listener {
	
	public static void register() {
		Bukkit.getPluginManager().registerEvents(new ParcelListener(), ParcelsPlugin.getInstance());
	}
	
	private static void checkBuildEvent(Cancellable event, Block b, Player user) {
		if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
			return;
		
		
		WorldManager.ifWorldPresent(b, (w, maybeP) -> {
			if (!maybeP.filter(p -> p.canBuild(user) && w.isInParcel(b.getX(), b.getZ(), p.getX(), p.getZ())).isPresent()) {
				cancel(event);
			}
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
		if (!user.hasPermission(Permissions.ADMIN_BYPASS)) {
			Location to = event.getTo();
			WorldManager.getWorld(to.getWorld()).ifPresent(world -> {
				world.getParcelAt(to.getBlockX(), to.getBlockZ()).filter(parcel -> parcel.getAdded().is(user.getUniqueId(), false)).ifPresent(() -> {
					Location from = event.getFrom();
					world.getParcelAt(from.getBlockX(), from.getBlockZ()).ifPresentOrElse(parcel -> {
						world.teleport(user, parcel);
						Messaging.send(user, "Parcels", Formatting.YELLOW, "You are banned from this parcel");
					}, () -> {
						event.setTo(event.getFrom());
					});
				});
			});
		}
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
	
	@EventHandler
	public void onFlow(BlockFromToEvent event) {
		if (!WorldManager.getParcel(event.getToBlock()).isPresent())
			cancel(event);
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {	

		Player user = event.getPlayer();
		
		WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
			
			boolean hasAdminPerm = user.hasPermission(Permissions.ADMIN_BUILDANYWHERE);
			
			Block clickedB = event.getClickedBlock();
			Optional<Parcel> clickedP = clickedB == null ? Optional.empty() : world.getParcelAt(clickedB.getX(), clickedB.getZ());
			
			Action action = event.getAction();
			switch (action) {
			case RIGHT_CLICK_BLOCK:
				
				Material type = clickedB.getType();
				switch(type) {
				case DIODE_BLOCK_ON:
				case DIODE_BLOCK_OFF:
				case REDSTONE_COMPARATOR_OFF:
				case REDSTONE_COMPARATOR_ON:
					if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user)).isPresent()) {
						cancel(event);
						return;
					}
					break;
				case LEVER:
				case STONE_BUTTON:
				case WOOD_BUTTON:
					if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user) || p.getSettings().allowsInteractLever()).isPresent()) {
						Messaging.send(user, "Parcels", Formatting.YELLOW, "You cannot use levers/buttons in this parcel");
						cancel(event);
						return;
					}
					break;
				default:
					break;
				}
				// no break
				
			case RIGHT_CLICK_AIR:
				
				if (event.hasItem()) {
					Material item = event.getItem().getType();
					if (world.getSettings().itemsBlocked.contains(event.getItem().getType())) {
						Messaging.send(user, "Parcels", Formatting.YELLOW, "That item is disabled in this world");
						cancel(event);
						return;
					} else if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user)).isPresent()) {
						switch(item) {
						case LAVA_BUCKET:
						case WATER_BUCKET:
						case BUCKET:
						case FLINT_AND_STEEL:
							cancel(event);
							break;
						default: 
							break;
						}
					}
				}
				break;
			
			case PHYSICAL:

				if (!hasAdminPerm && !clickedP.filter(p -> p.canBuild(user) || p.getSettings().allowsInteractLever()).isPresent()) {
					cancel(event);
					return;
				}
				break;	
				
			default:
				break;
			}
		});
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

		Player user = event.getPlayer();
		
		WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
			if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE) || world.getParcelAt(user.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
				return;
			}
			
			switch (event.getRightClicked().getType()) {
			case BOAT:
			case MINECART:
			case MINECART_CHEST:
			case MINECART_COMMAND:
			case MINECART_FURNACE:
			case MINECART_HOPPER:
			case MINECART_MOB_SPAWNER:
			case MINECART_TNT:		
				
			case ARMOR_STAND:
			case ITEM_FRAME:
			case LEASH_HITCH:		
				
			case CHICKEN:
			case COW:
			case HORSE:
			case SHEEP:
			case VILLAGER:
			case WOLF:
				cancel(event);
				break;
			default:
				break;
			}
		});
	}
	
	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {

		Entity e = event.getEntity();
		if (WorldManager.getWorld(e.getWorld()).isPresent() && e.getType() == EntityType.ENDERMAN) {
			cancel(event);
		}
	}
	
	@EventHandler
	public void onEntityCreatePortal(EntityCreatePortalEvent event) {

		WorldManager.getWorld(event.getEntity().getWorld()).filter(world -> world.getSettings().blockPortalCreation).ifPresent(() -> cancel(event));
	}
	
	@EventHandler
	public void onInventoryInteract(InventoryInteractEvent event) {

		WorldManager.getWorld(event.getWhoClicked().getWorld()).ifPresent(world -> {
			ParcelsPlugin.debug("InventoryInteract");
			
			Player user = (Player) event.getWhoClicked();
			if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
				return;
			
			InventoryHolder inv = event.getInventory().getHolder();
			Location loc;
			if (inv instanceof BlockState) {
				ParcelsPlugin.debug("BlockState");
				loc = ((BlockState) inv).getLocation();
			} else if (inv instanceof Entity) {
				ParcelsPlugin.debug("Entity");
				loc = ((Entity) inv).getLocation();
			} else {
				ParcelsPlugin.debug("Nothing");
				return;
			}
			
			if (!world.getParcelAt(loc).filter(p -> p.canBuild(user) || p.getSettings().allowsInteractInventory()).isPresent()) {
				cancel(event);
			}
		});
	}
	
	private int ignoreWeatherChanges = 0;
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onWeatherChange(WeatherChangeEvent event) {
		WorldManager.getWorld(event.getWorld()).filter(world -> world.getSettings().staticWeatherClear).ifPresent(world -> {
			if (ignoreWeatherChanges > 0) {
				ignoreWeatherChanges--;
			} else {
				ignoreWeatherChanges = 1;
				cancel(event);
				World w = world.getWorld();
				w.setStorm(false);
				w.setThundering(false);
				w.setWeatherDuration(Integer.MAX_VALUE);
			}
		});
	}
	
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		WorldManager.getWorld(event.getWorld()).ifPresent(world -> {
			World w = world.getWorld();
			
			if (world.getSettings().staticTimeDay) {
				w.setGameRuleValue("doDaylightCycle", "false");
				w.setTime(6000);
			}
			
			if (world.getSettings().staticWeatherClear) {
				ignoreWeatherChanges = 1;
				w.setStorm(false);
				w.setThundering(false);
				w.setWeatherDuration(Integer.MAX_VALUE);
			}
		});
	}
		
	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent event) {
		Entity e = event.getEntity();
		WorldManager.getWorld(e.getWorld()).filter(world -> e instanceof LivingEntity && world.getSettings().blockMobSpawning).ifPresent(() -> cancel(event));
	}
	
	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		if (!WorldManager.getParcel(event.getTo()).isPresent()) {
			Vehicle vehicle = event.getVehicle();
			Entity passenger = vehicle.getPassenger();
			if (passenger != null) {
				vehicle.eject();
				passenger.remove();
				if (passenger.getType() == EntityType.PLAYER) {
				vehicle.eject();
				Messaging.send((Player)passenger, "Parcels", Formatting.RED, "Your ride ends here");
				}
			}
			vehicle.remove();
		}
			
	}

}
