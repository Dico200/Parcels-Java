package com.redstoner.parcels.generation;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import com.redstoner.utils.Values;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import com.redstoner.parcels.api.ParcelWorldSettings;

public class ParcelGenerator extends ChunkGenerator {
	
	public ParcelGenerator(ParcelWorldSettings pws) {	
		this.floorId = pws.floorType.getId();
		this.wallId = pws.wallType.getId();
		this.pathMainId = pws.pathMainType.getId();
		this.pathEdgeId = pws.pathEdgeType.getId();
		this.fillId = pws.fillType.getId();
		
		this.floorData = pws.floorType.getData();
		this.wallData = pws.wallType.getData();
		this.pathMainData = pws.pathMainType.getData();
		this.pathEdgeData = pws.pathEdgeType.getData();
		this.fillData = pws.fillType.getData();
		
		this.parcelSize = pws.parcelSize;
		this.floorHeight = pws.floorHeight;
		this.xOffset = pws.offsetX;
		this.zOffset = pws.offsetZ;
		this.sectionSize = pws.sectionSize;
		this.pathOffset = pws.pathOffset;
		
		int pathWidth = sectionSize - parcelSize;
		this.makePathEdge = pathWidth > 4;
		this.makePathMain = pathWidth > 2;
	}
	
	private final short floorId, wallId, pathMainId, pathEdgeId, fillId;
	private final byte floorData, wallData, pathMainData, pathEdgeData, fillData;
	private final int parcelSize, floorHeight, xOffset, zOffset, sectionSize, pathOffset;
	private final boolean makePathMain, makePathEdge;
	
	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		double fix = ((sectionSize - parcelSize) % 2 == 0)? 0.5 : 0;
		return new Location(world, xOffset + fix, floorHeight + 1, zOffset + fix);
	}
	
	/*
	public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
		ChunkData chunk = createChunkData(world);
		
		return null;
		
	}
	*/
	
	@Override
	public short[][] generateExtBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
		
		//Bool.validate(floorHeight <= world.getMaxHeight(), "The floor level may not be higher than the world's maximum height");
		//Bool.validate(world.getMaxHeight() >> 4 == 0, "The world's max height must be a multiple of 16");
		
		short[][] chunk = IntStream.range(0, 16).mapToObj(i -> new short[4096]).toArray(size -> new short[size][]);
		
		//[floor(y / 16)][y % 16 * 256 + z * 16 + x]
		iterAll(chunkX, chunkZ, floorId, wallId, pathMainId, pathEdgeId, fillId, (c, type) -> {
			chunk[c.y >> 4][((c.y & 0xF) << 8) | (c.z << 4) | c.x] = type;
		});
		
		return chunk;
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		return Arrays.asList(new BlockPopulator[]{new BlockPopulator() {
	
			@SuppressWarnings("deprecation")
			@Override
			public void populate(World world, Random random, Chunk chunk) {
				iterAll(chunk.getX(), chunk.getZ(), floorData, wallData, pathMainData, pathEdgeData, fillData, (c, type) -> {
					if (c.y == 1) {
						Block b = chunk.getBlock(c.x, c.y, c.z);
						b.setData(type);
						b.setBiome(Biome.JUNGLE);
					} else {
						chunk.getBlock(c.x, c.y, c.z).setData(type);
					}
				});
			}
			
		}});
	}
	
	public <T> void iterAll(int chunkX, int chunkZ, T floor, T wall, T pathMain, T pathEdge, T fill, BiConsumer<Coord, T> forEach) {
		
		//northwest chunk corner, with offset applied -> x/z
		//The offset makes it believe it's generating for coordinates at offset back. Like nudging a graph: (x - offset)
		//modulo sectionSize with positive outcome
		//subtract pathOffset such that (xOffset, zOffset) is the center of a path intersection, the northwest part of it if pathwidth is even
		int x, z;
		final int x0 = ((x = (((chunkX << 4) - xOffset) % sectionSize)) < 0)? x + sectionSize : x;
		final int z0 = ((z = (((chunkZ << 4) - zOffset) % sectionSize)) < 0)? z + sectionSize : z;
		
		int cX, y, cZ, height;
		for (cX = 0; cX < 16; cX++) {
			for (cZ = 0; cZ < 16; cZ++) {
				x = (x0 + cX) % sectionSize - pathOffset;
				z = (z0 + cZ) % sectionSize - pathOffset;
				height = floorHeight;
				
				T type;
				if (Values.inRange(x, 0, parcelSize) && Values.inRange(z, 0, parcelSize)) {
					type = floor;
				} else if (Values.inRange(x, -1, parcelSize + 1) && Values.inRange(z, -1, parcelSize + 1)) {
					type = wall;
					height += 1;
				} else if (Values.inRange(x, -2, parcelSize + 2) && Values.inRange(z, -2, parcelSize + 2) && makePathEdge) {
					type = pathEdge;
				} else if (makePathMain) {
					type = pathMain;
				} else {
					type = wall;
				}
				
				forEach.accept(new Coord(cX, height, cZ), type);
				for (y = 0; y < height; y++) {
					forEach.accept(new Coord(cX, y, cZ), fill);
				}
			}
		}
	}
}

class Coord {
	
	Coord(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	int x, y, z;

}
