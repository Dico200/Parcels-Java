package com.redstoner.parcels.generation;

import java.util.Random;
import java.util.stream.IntStream;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import com.redstoner.parcels.api.ParcelWorldSettings;
import com.redstoner.utils.Bool;
import com.redstoner.utils.Calc;

public class ParcelGenerator extends ChunkGenerator {
	
	public ParcelGenerator(ParcelWorldSettings pws) {
		this.pws = pws;
	}
	
	private ParcelWorldSettings pws;
	
	@Override
	public short[][] generateExtBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
		
		assert pws.floorHeight <= world.getMaxHeight(): "The floor level may not be higher than the world's maximum height";
		assert world.getMaxHeight() >> 4 == 0: "The world's max height must be a multiple of 16";
		
		short[][] chunk = IntStream.range(0, world.getMaxHeight() / 16)
								   .mapToObj(i -> new short[4096])
								   .toArray(size -> new short[size][]);

		final int x0 = Calc.posModulo(16 * chunkX, pws.sectionSize);
		final int z0 = Calc.posModulo(16 * chunkZ, pws.sectionSize);

		for (int cX = 0; cX < 16; cX++) {
			for (int cZ = 0; cZ < 16; cZ++) {
				int x = (x0 + cX) % pws.sectionSize;
				int z = (z0 + cZ) % pws.sectionSize;
				int x2 = x - pws.xOffset;
				int z2 = z - pws.zOffset;
				int height = pws.floorHeight;
				
				short type;
				if (Bool.inRange(x2, 0, pws.parcelSize) && Bool.inRange(z2, 0, pws.parcelSize)) {
					type = pws.floor;
				} else if (Bool.inRange(x2, -1, pws.parcelSize + 1) && Bool.inRange(z2, -1, pws.parcelSize + 1)) {
					type = pws.wall;
					height += 1;
				} else if (Bool.inRange(x2, -2, pws.parcelSize + 2) && Bool.inRange(z2, -2, pws.parcelSize + 2)) {
					type = pws.pathEdge;
 				} else {
					type = pws.pathMain;
				}

				setBlock(chunk, cX, height, cZ, type);
				
				for (int y = 0; y < height; y++) {
					setBlock(chunk, cX, y, cZ, pws.fill);
				}
			}
		}
		
		return chunk;
	}
	
    private static void setBlock(short[][] result, int x, int y, int z, short id) {
        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = id;
    }

}
