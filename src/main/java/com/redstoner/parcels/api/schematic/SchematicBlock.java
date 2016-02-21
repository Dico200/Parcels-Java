package com.redstoner.parcels.api.schematic;

import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import com.redstoner.parcels.ParcelsPlugin;

class SchematicBlock {
	
	private final int x, y, z;
	private final int typeId;
	private final byte data;
	private final Consumer<BlockState> converter;
	
	@SuppressWarnings("deprecation")
	public SchematicBlock(Block block) {
		this.x = block.getX();
		this.y = block.getY();
		this.z = block.getZ();
		this.typeId = (short) block.getTypeId();
		this.data = block.getData();
		this.converter = SpecialType.getConverter(block.getState());
	}
	
	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	@SuppressWarnings("deprecation")
	public void paste(World world, int relativeX, int relativeY, int relativeZ) {
	    Block block = world.getBlockAt(x + relativeX, y + relativeY, z + relativeZ);
		block.setTypeIdAndData(typeId, data, false);
		if (converter != null) {
    		try {
    		    BlockState state = block.getState();
        		converter.accept(state);
        		state.update();
    		} catch (ClassCastException e) {
    		    ParcelsPlugin.debug("Failed to update a block properly in swap");
    		}
		}
	}
	
	public String toString() {
		return String.format("SchematicBlock{typeId:%s|data:%s}", typeId, data);
	}
	
}
