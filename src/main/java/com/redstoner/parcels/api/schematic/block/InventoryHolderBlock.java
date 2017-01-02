package com.redstoner.parcels.api.schematic.block;

import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class InventoryHolderBlock<T extends BlockState & InventoryHolder> extends StateBlock<T> {
    private ItemStack[] contents;

    public InventoryHolderBlock(T state) {
        super(state);
        contents = state.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                contents[i] = new ItemStack(contents[i]);
            }
        }
    }

    public ItemStack[] getContents() {
        return contents;
    }

    @Override
    public Type getType() {
        return Type.INVENTORY_HOLDER;
    }

    @Override
    protected void paste(T state) {
        state.getInventory().setContents(contents);
    }

}
