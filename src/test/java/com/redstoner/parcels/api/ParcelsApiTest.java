package com.redstoner.parcels.api;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.junit.*;

import com.redstoner.parcels.ParcelsPlugin;
import com.redstoner.utils.DuoObject.Coord;
import com.redstoner.utils.MultiRunner;
import com.redstoner.utils.mysql.SqlConnector;

public class ParcelsApiTest {
	
	private static SqlConnector connector;
	private static String worldName;
	private static ParcelWorld world;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		
		String host = "localhost:3306";
		String database = "redstoner";
		String username = "root";
		String password = "";
		
		connector = new SqlConnector(host, database, username, password);
		
		worldName = "testWorld";
		
	}
	
	@Test
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
				put("interaction.items-blocked", Arrays.asList(new String[]{"FLINT_AND_STEEL"}));
				
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
		
		MultiRunner errorPrinter = new MultiRunner(() -> {
			ParcelsPlugin.log("##########################################################");
			ParcelsPlugin.log(String.format("Exception(s) occurred while loading settings for world '%s':", worldName));
		}, () -> {
			ParcelsPlugin.log("##########################################################");
		});
		
		ParcelWorldSettings parsedSettings = ParcelWorldSettings.parseMap(worldName, settings, errorPrinter);
		errorPrinter.runAll();
		assertNotNull(parsedSettings);
		
		world = new ParcelWorld(worldName, parsedSettings);
		
		ParcelWorldSettings fromWorld = world.getSettings();
		assertEquals(fromWorld.axisLimit, 10);
		assertEquals(fromWorld.staticTimeDay, true);
		assertEquals(fromWorld.staticWeatherClear, true);
		assertEquals(fromWorld.disableExplosions, true);
		assertEquals(fromWorld.blockPortalCreation, true);
		assertEquals(fromWorld.blockMobSpawning, true);
		assertEquals(fromWorld.blockedItems, Arrays.asList(new Material[]{Material.FLINT_AND_STEEL}));
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
		
		SqlManager.initialise(connector);
		
		testParcels();

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
		
		//testInteractionSettings(p);
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
	
	private void testInteractionSettings(Parcel parcel) {
		System.out.println("Testing interaction settings for " + parcel.toString());
		
		SqlManager.setAllowInteractInputs(worldName, parcel.getX(), parcel.getZ(), true);
		SqlManager.setAllowInteractInventory(worldName, parcel.getX(), parcel.getZ(), true);
		
		sleepThread();
		connector.asyncConn(conn -> {
			SqlManager.loadAllFromDatabase(conn);
		});
		sleepThread();
		
		assertEquals(parcel.getSettings().allowsInteractInputs(), true);
		assertEquals(parcel.getSettings().allowsInteractInventory(), true);
		
		SqlManager.setAllowInteractInputs(worldName, parcel.getX(), parcel.getZ(), false);
		SqlManager.setAllowInteractInventory(worldName, parcel.getX(), parcel.getZ(), false);
		sleepThread();
		connector.asyncConn(conn -> {
			SqlManager.loadAllFromDatabase(conn);
		});
		sleepThread();
		
		assertEquals(parcel.getSettings().allowsInteractInputs(), false);
		assertEquals(parcel.getSettings().allowsInteractInventory(), false);
	}

}
