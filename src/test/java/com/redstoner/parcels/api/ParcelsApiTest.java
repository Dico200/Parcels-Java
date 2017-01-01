package com.redstoner.parcels.api;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.parcels.api.fake.FakeServer;
import com.redstoner.parcels.api.storage.SqlManager;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.ErrorPrinter;
import com.redstoner.utils.sql.MySQLConnector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParcelsApiTest {
	
	public static final UUID fakeUUID1 = UUID.fromString("51f2ad3c-6cc8-40ea-aa2b-f25970316921");
	public static final UUID fakeUUID2 = UUID.fromString("e78eb639-1076-45df-a50c-ba349b122280");
	
	private static MySQLConnector connector;
	private static String worldName;
	private static ParcelWorld world;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		
		String host = "localhost:3306";
		String database = "test";
		String username = "root";
		String password = "";
		
		connector = new MySQLConnector(host, database, username, password);
		
		/*
		 * DO NOT ENTER THE NAME OF A USED PARCEL WORLD, UNLESS THE PLOTME WORLD IS THERE TOO (which is likely...)
		 * (Well, it doesn't matter if you use a different db but it's DANGEROUS)
		 */
		worldName = "junitTestWorld";
		new FakeServer();
		
	}
	
	@Test
	public void startTest() {
		testEverything();
	}
	
	public void testEverything() {
		
		Map<String, Object> settings = new HashMap<String, Object>() {
			private static final long serialVersionUID = 1L;
			
			{
				put("parcel-axis-limit", 10);
				put("static-time-day", true);
				put("static-weather-clear", true);
				
				put("interaction.disable-explosions", true);
				put("interaction.block-portal-creation", true);
				put("interaction.block-mob-spawning", true);
				put("interaction.items-blocked", Collections.singletonList("FLINT_AND_STEEL"));
				
				put("generator.wall-type", "35:6");
				put("generator.floor-type", "24:1");
				put("generator.fill-type", "24");
				put("generator.path-main-type", "155:2");
				put("generator.path-edge-type", "152");
				put("generator.parcel-size", 50);
				put("generator.path-size", 8);
				put("generator.floor-height", 100);
				put("generator.offset-x", 2);
				put("generator.offset-z", -3);
			}
			
		};
		
		ErrorPrinter errorPrinter = new ErrorPrinter(ParcelsPlugin.getInstance()::error,
				String.format("Exception(s) occurred while loading settings for world '%s':", worldName));
		
		ParcelWorldSettings parsedSettings = ParcelWorldSettings.parseMap(worldName, settings, errorPrinter);
		errorPrinter.runAll();
		assertNotNull(parsedSettings);
		
		world = new ParcelWorld(worldName, parsedSettings);
		WorldManager.getWorlds().put(worldName, world);
		
		ParcelWorldSettings fromWorld = world.getSettings();
		assertEquals(fromWorld.axisLimit, 10);
		assertEquals(fromWorld.staticTimeDay, true);
		assertEquals(fromWorld.staticWeatherClear, true);
		assertEquals(fromWorld.disableExplosions, true);
		assertEquals(fromWorld.blockPortalCreation, true);
		assertEquals(fromWorld.blockMobSpawning, true);
		assertEquals(fromWorld.blockedItems, Collections.singletonList(Material.FLINT_AND_STEEL));
		assertEquals(fromWorld.wallType.getId(), 35);
		assertEquals(fromWorld.wallType.getData(), 6);
		assertEquals(fromWorld.floorType.getId(), 24);
		assertEquals(fromWorld.floorType.getData(), 1);
		assertEquals(fromWorld.fillType.getId(), 24);
		assertEquals(fromWorld.fillType.getData(), 0);
		assertEquals(fromWorld.pathMainType.getId(), 155);
		assertEquals(fromWorld.pathMainType.getData(), 2);
		assertEquals(fromWorld.pathEdgeType.getId(), 152);
		assertEquals(fromWorld.parcelSize, 50);
		assertEquals(fromWorld.sectionSize, 58);
		assertEquals(fromWorld.floorHeight, 100);
		assertEquals(fromWorld.offsetX, 2);
		assertEquals(fromWorld.offsetZ, -3);
		
		SqlManager.initialise(connector, true, false);
		
		try {
			
			testParcels();
			
		} finally {
		
			connector.asyncConn(conn -> {
				try {
					PreparedStatement update = conn.prepareStatement("DELETE FROM `parcels` WHERE `world` = ?;");
					update.setString(1, worldName);
					update.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
			
		}

	}
	
	private void sleepThread() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void testParcels() {
		testParcel(0, 0);
		testParcel(2, 2);
		testParcel(-1, -1);
		testParcel(-1, 0);
		testParcel(-1, 1);
	}
	
	private void testParcel(int idx, int idz) {
		Parcel p = world.getParcelByID(idx, idz);
		
		ParcelWorldSettings settings = world.getSettings();
		int bottomX = settings.sectionSize * idx + settings.pathOffset + settings.offsetX;
		int bottomZ = settings.sectionSize * idz + settings.pathOffset + settings.offsetZ;
		
		Coord bottom = world.getBottomCoord(p);
		assertEquals(bottom.getX(), bottomX);
		assertEquals(bottom.getZ(), bottomZ);
		
		assertEquals(world.getParcelAt(bottomX, bottomZ).orElse(null), p);
		assertEquals(world.getParcelAt(bottomX + settings.sectionSize, bottomZ + settings.sectionSize).orElse(null), world.getParcelByID(idx + 1, idz + 1));
		assertEquals(world.getParcelAt(bottomX - 2*settings.sectionSize, bottomZ - 2*settings.sectionSize).orElse(null), world.getParcelByID(idx - 2, idz -2));
		assertEquals(world.getParcelAt(bottomX + settings.parcelSize - 1, bottomZ + settings.parcelSize - 1).orElse(null), world.getParcelByID(idx, idz));
		
		checkWorldBlockId(bottomX - 1, settings.floorHeight + 1, bottomZ - 1, settings.wallType.getId());
		checkWorldBlockId(bottomX, settings.floorHeight, bottomZ, settings.floorType.getId());
		checkWorldBlockId(bottomX, settings.floorHeight - 1, bottomZ, settings.fillType.getId());
		checkWorldBlockId(bottomX - 2, settings.floorHeight, bottomZ - 2, settings.pathEdgeType.getId());
		
		testSQL(p.getX(), p.getZ());
	}
	
	private void checkWorldBlockId(int x, int y, int z, short type) {
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		int remainderX = x & 0xF;
		int remainderZ = z & 0xF;
		
		short[][] bottomChunk = world.getGenerator().generateExtBlockSections(null, null, chunkX, chunkZ, null);
		
		int ydiv16 = y >> 4;
		int ymod16 = y & 0xF;
		
		short result = bottomChunk[ydiv16][(ymod16 << 8) | (remainderZ << 4) | remainderX];
		System.out.println(String.format("Checked if block at (%s,%s,%s) is of type %s. Result: %s", x, y, z, type, result));
		assertEquals(result, type);
	}
	
	private void testSQL(int idx, int idz) {
		Parcel parcel = world.getParcelByID(idx, idz);
		
		System.out.println("Testing SQL for " + parcel.toString());
		
		SqlManager.setOwner(worldName, parcel.getX(), parcel.getZ(), fakeUUID1);
		SqlManager.addPlayer(worldName, parcel.getX(), parcel.getZ(), fakeUUID2, false);
		SqlManager.setAllowInteractInputs(worldName, parcel.getX(), parcel.getZ(), true);
		SqlManager.setAllowInteractInventory(worldName, parcel.getX(), parcel.getZ(), true);
		
		sleepThread();
		connector.asyncConn(SqlManager::loadAllFromDatabase);
		sleepThread();
		
		// New object because the container was reset. (by the test only)
		parcel = world.getParcelByID(idx, idz);
		
		assertEquals(true, parcel.isOwner(Bukkit.getOfflinePlayer(fakeUUID1)));
		assertEquals(true, parcel.isBanned(Bukkit.getOfflinePlayer(fakeUUID2)));
		assertEquals(false, parcel.isAllowed(Bukkit.getOfflinePlayer(fakeUUID1)));
		assertEquals(false, parcel.isAllowed(Bukkit.getOfflinePlayer(fakeUUID2)));
		assertEquals(true, parcel.getSettings().allowsInteractInputs());
		assertEquals(true, parcel.getSettings().allowsInteractInventory());
		
		SqlManager.removePlayer(worldName, parcel.getX(), parcel.getZ(), fakeUUID2);
		SqlManager.setOwner(worldName, parcel.getX(), parcel.getZ(), fakeUUID2);
		SqlManager.addPlayer(worldName, parcel.getX(), parcel.getZ(), fakeUUID1, true);
		SqlManager.setAllowInteractInputs(worldName, parcel.getX(), parcel.getZ(), false);
		SqlManager.setAllowInteractInventory(worldName, parcel.getX(), parcel.getZ(), false);
		
		sleepThread();
		connector.asyncConn(SqlManager::loadAllFromDatabase);
		sleepThread();
		
		parcel = world.getParcelByID(idx, idz);
		
		System.out.println("Added players:");
		parcel.getAdded().getMap().forEach((player, value) -> {
			System.out.println(String.format("player %s is allowed: %s", player, value));
		});
		
		assertEquals(false, parcel.isOwner(Bukkit.getOfflinePlayer(fakeUUID1)));
		assertEquals(false, parcel.isBanned(Bukkit.getOfflinePlayer(fakeUUID2)));
		assertEquals(true, parcel.isAllowed(Bukkit.getOfflinePlayer(fakeUUID1)));
		assertEquals(false, parcel.isAllowed(Bukkit.getOfflinePlayer(fakeUUID2)));
		assertEquals(false, parcel.getSettings().allowsInteractInputs());
		assertEquals(false, parcel.getSettings().allowsInteractInventory());
	}

}
