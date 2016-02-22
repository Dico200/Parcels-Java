package com.redstoner.parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
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
	
	private static void cancel(Cancellable event) {
		event.setCancelled(true);
	}
	
	private int ignoreWeatherChanges;
	private final ConcurrentHashMap<Entity, Parcel> entities;
	
	private ParcelListener() {
		ignoreWeatherChanges = 0;
		entities = new ConcurrentHashMap<>();
		
		/*
		 * Tracks entities. If the entity is dead, they are removed from the list.
		 * If the entity is found to have left the parcel it was created in, it will be removed from the world and from the list.
		 * If it is still in the parcel it was created in, and it is on the ground, it is removed from the list.
		 * 
		 * Start after 5 seconds, run every 0.25 seconds
		 */
		Bukkit.getScheduler().scheduleSyncRepeatingTask(ParcelsPlugin.getInstance(), () -> {
			entities.forEach((entity, firedFrom) -> {
				if (entity.isDead()) {
					entities.remove(entity);
				} else if (WorldManager.getParcel(entity.getLocation()).orElse(null) != firedFrom) {
					entity.remove();
					entities.remove(entity);
				} else if (entity.isOnGround()) {
					entities.remove(entity);
				}
			});
		}, 100, 5);
	}
	
	private static Location inventoryLocation(Inventory inv) {
		InventoryHolder holder = inv.getHolder();
		if (holder instanceof BlockState) {
			return ((BlockState) holder).getLocation();
		} else if (inv instanceof Entity) {
			return ((Entity) inv).getLocation();
		} else {
			return null;
		}
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
	
	/*
	 * Prevents players from entering plots they are banned from
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerMove(PlayerMoveEvent event) {
		
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
	
	/*
	 * Prevents players from breaking blocks outside of parcels
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {

		checkBuildEvent(event, event.getBlock(), event.getPlayer());
	}
	
	/*
	 * Prevents players from placing blocks outside of parcels
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {

		checkBuildEvent(event, event.getBlockPlaced(), event.getPlayer());
	}
	
	/*
	 * Prevents pistons from touching blocks outside parcels
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		
		checkPistonAction(event, event.getBlocks());
	}
	
	/*
	 * Prevents pistons from touching blocks outside parcels
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		
		checkPistonAction(event, event.getBlocks());
	}
	
	/*
	 * Prevents explosions if enabled by the configs for that world
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		
		WorldManager.getWorld(event.getLocation().getWorld()).filter(w -> w.getSettings().disableExplosions).ifPresent(() -> cancel(event));
	}
	
	/*
	 * Prevents liquids from flowing outside of parcels
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		
		if (!WorldManager.getParcel(event.getToBlock()).isPresent())
			cancel(event);
	}
	
	/*
	 * Prevents players from placing liquids, using flint and steel, changing redstone components,
	 * clicking levers buttons stepping on pressure plates (unless allowed by the plot), 
	 * and using items disabled in the configuration for that world.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {	

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
	
	/*
	 * Prevents players from breeding mobs, entering or opening boats/minecarts, 
	 * rotating item frames, doing stuff with leashes, and putting stuff on armor stands.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

		Player user = event.getPlayer();
		if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
			return;
		
		WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
			if (world.getParcelAt(user.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
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
			case PAINTING:
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
	
	/*
	 * Prevents endermen from griefing.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {

		Entity e = event.getEntity();
		if (WorldManager.getWorld(e.getWorld()).isPresent() && e.getType() == EntityType.ENDERMAN) {
			cancel(event);
		}
	}
	
	/*
	 * Prevents portals from being created if set so in the configs for that world
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityCreatePortal(EntityCreatePortalEvent event) {

		WorldManager.getWorld(event.getEntity().getWorld()).filter(world -> world.getSettings().blockPortalCreation).ifPresent(() -> cancel(event));
	}
	
	/*
	 * Prevents players from dropping items
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		
		Player user = event.getPlayer();
		if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
			return;
		
		WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
			
			Item item = event.getItemDrop();
			if (!world.getParcelAt(item.getLocation()).filter(p -> p.canBuild(user) || p.getSettings().allowsInteractInventory()).isPresent()) {
				cancel(event);
			}
		});
	}
	
	/*
	 * Prevents players from editing inventories
	 */
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		
		Player user = (Player) event.getWhoClicked();
		if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
			return;
		
		WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
			Inventory inv = event.getInventory();
			if (inv == null) // if hotbar, returns null
				return;
			
			InventoryHolder holder = inv.getHolder();
			if (holder instanceof Entity && ((Entity) holder).getType() == EntityType.PLAYER)
				return;
			
			Location loc = inventoryLocation(inv);
			if (loc == null)
				return;
			
			if (!world.getParcelAt(loc).filter(p -> p.canBuild(user) || p.getSettings().allowsInteractInventory()).isPresent()) {
				cancel(event);
			}
		});
	}
	
	
	/*
	 * Cancels weather changes and sets the weather to sunny if requested by the config for that world.
	 */
	private void resetWeather(World w) {
		ignoreWeatherChanges++;
		w.setStorm(false);
		w.setThundering(false);
		w.setWeatherDuration(Integer.MAX_VALUE);
	}
	
	@EventHandler
	public void onWeatherChange(WeatherChangeEvent event) {
		WorldManager.getWorld(event.getWorld()).filter(world -> world.getSettings().staticWeatherClear).ifPresent(world -> {
			if (ignoreWeatherChanges > 0) {
				ignoreWeatherChanges--;
			} else {
				cancel(event);
				resetWeather(world.getWorld());
			}
		});
	}
	
	/*
	 * Sets time to day and doDayLightCycle gamerule if requested by the config for that world
	 * Sets the weather to sunny if requested by the config for that world.
	 */
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		WorldManager.getWorld(event.getWorld()).ifPresent(world -> {
			World w = world.getWorld();
			
			if (world.getSettings().staticTimeDay) {
				w.setGameRuleValue("doTileDrops", "false");
				w.setGameRuleValue("doDaylightCycle", "false");
				w.setTime(6000);
			}
			
			if (world.getSettings().staticWeatherClear) {
				resetWeather(w);
			}
		});
	}
	
	/*
	 * Prevents mobs (living entities) from spawning if that is disabled for that world in the config.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntitySpawn(EntitySpawnEvent event) {
		
		Entity e = event.getEntity();
		WorldManager.getWorld(e.getWorld()).filter(world -> e instanceof LivingEntity && world.getSettings().blockMobSpawning).ifPresent(() -> cancel(event));
	}
	
	/*
	 * Prevents minecarts/boats from moving outside a plot
	 */
	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		
		if (!WorldManager.getParcel(event.getTo()).isPresent()) {
			Vehicle vehicle = event.getVehicle();
			Entity passenger = vehicle.getPassenger();
			if (passenger != null) {
				vehicle.eject();
				if (passenger.getType() == EntityType.PLAYER) {
					vehicle.eject();
					Messaging.send((Player)passenger, "Parcels", Formatting.RED, "Your ride ends here");
				} else {
					passenger.remove();
				}
			}
			vehicle.remove();
		}
			
	}
	
	/*
	 * Prevents players from removing items from item frames
	 */
	@EventHandler 
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		
		Entity e = event.getEntity();
		if (!(e.getType() == EntityType.ITEM_FRAME)) {
			return;
		}
		
		WorldManager.getWorld(e.getWorld()).ifPresent(world -> {
			Entity damager = event.getDamager();
			Player user;
			if (damager instanceof Player) {
				user = (Player) damager;
			} else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
				user = (Player) ((Projectile) damager).getShooter();
			} else {
				return;
			}
			
			if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
				return;
			}
			
			if (!world.getParcelAt(e.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
				cancel(event);
			}
		});
	}
	
	/*
	 * Prevents players from deleting paintings and item frames
	 * This appears to take care of shooting with a bow, throwing snowballs or throwing ender pearls.
	 */
	@EventHandler
	public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
		
		Entity hanging = event.getEntity();
		WorldManager.getWorld(hanging.getWorld()).ifPresent(world -> {
			
			Entity remover = event.getRemover();
			if (remover instanceof Player) {
				ParcelsPlugin.debug("Player found");
				Player user = (Player) remover;
				
				if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE)) {
					return;
				}
				ParcelsPlugin.debug("Checking build permissions");
				if (!world.getParcelAt(hanging.getLocation()).filter(p -> p.canBuild(user)).isPresent()) {
					cancel(event);
				}
			}
		});
		
	}
	
	/*
	 * Prevents players from placing paintings and item frames
	 */
	@EventHandler(ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent event) {
		
		Player user = event.getPlayer();
		if (user.hasPermission(Permissions.ADMIN_BUILDANYWHERE))
			return;
		
		WorldManager.getWorld(user.getWorld()).ifPresent(world -> {
			Block b = event.getBlock().getRelative(event.getBlockFace());
			if (!world.getParcelAt(b).filter(p -> p.canBuild(user)).isPresent()) {
				cancel(event);
			}
		});
		
	}
	
	/*
	 * Prevents stuff from growing outside of plots
	 */
	@EventHandler
	public void onStructureGrow(StructureGrowEvent event) {
		
		WorldManager.getWorld(event.getLocation().getWorld()).ifPresent(world -> {
			
			world.getParcelAt(event.getLocation()).ifPresentOrElse(parcel -> {
				
				Player user = event.getPlayer();
				if (!user.hasPermission(Permissions.ADMIN_BUILDANYWHERE) && !parcel.canBuild(user)) {
					cancel(event);
					return;
				}
				
				List<BlockState> blocks = event.getBlocks();
				for (BlockState block : new ArrayList<>(blocks)) {
					if (!world.getParcelAt(block.getBlock()).isPresent()) {
						blocks.remove(block);
					}
				}
			}, () -> {
				cancel(event);
			});
			
		});
	}
	
	/*
	 * Prevents dispensers/droppers from dispensing out of parcels
	 */
	private static BlockFace getDispenserFace(byte data) {
		switch (data) {
		case 0:
		case 6:
		case 8:
		case 14:
			return BlockFace.DOWN;
		case 1:
		case 7:
		case 9:
		case 15:
			return BlockFace.UP;
		case 2:
		case 10:
			return BlockFace.NORTH;
		case 3:
		case 11:
			return BlockFace.SOUTH;
		case 4:
		case 12:
			return BlockFace.WEST;
		case 5:
		case 13:
			return BlockFace.EAST;
		default:
			return null;
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onBlockDispense(BlockDispenseEvent event) {
		
		Block block = event.getBlock();
		if (block.getType() != Material.DISPENSER && block.getType() != Material.DROPPER)
			return;
		
		WorldManager.getWorld(block).ifPresent(world -> {
			
			if (!world.getParcelAt(block.getRelative(getDispenserFace(block.getData()))).isPresent()) {
				cancel(event);
			}
		});
	}
	
	/*
	 * Puts spawned items into the entities list, making sure they don't leave the parcel.
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onItemSpawn(ItemSpawnEvent event) {
		
		Item item = event.getEntity();
		WorldManager.getWorld(item.getWorld()).ifPresent(world -> {
			
			Parcel parcelFrom = world.getParcelAt(item.getLocation()).orElse(null);
			if (parcelFrom == null) {
				cancel(event);
			} else {
				entities.put(item, parcelFrom);
			}
		});
	}
	
	/*
	 * Prevents endermen and endermite from teleporting outside their parcel
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityTeleport(EntityTeleportEvent event) {
		
		Location from = event.getFrom();
		WorldManager.getWorld(from.getWorld()).ifPresent(world -> {
			world.getParcelAt(from).ifPresent(parcelFrom -> {
				if (!world.getParcelAt(event.getTo()).filter(parcelTo -> parcelTo == parcelFrom).isPresent()) {
					cancel(event);
				}
			});
		});
	}
	
	/*
	 * Prevents projectiles from flying out of parcels
	 * Prevents players from firing projectiles if they cannot build
	 */
	@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent event) {
		
		Projectile arrow = event.getEntity();
		WorldManager.getWorld(arrow.getWorld()).ifPresent(world -> {
			
			Parcel firedFrom = world.getParcelAt(arrow.getLocation()).orElse(null);
			if (firedFrom == null || (arrow.getShooter() instanceof Player && !firedFrom.canBuild((Player) arrow.getShooter()))) {
				cancel(event);
			} else {
				entities.put(arrow, firedFrom);
			}
		});	
	}

}
