package com.redstoner.parcels.api.schematic.block;

import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;

import java.util.List;

public class BannerBlock extends StateBlock<Banner> {
    private List<Pattern> patterns;

    public BannerBlock(Banner state) {
        super(state);
        // returns copied list of immutable Pattern objects
        patterns = state.getPatterns();
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public Type getType() {
        return Type.BANNER;
    }

    @Override
    protected void paste(Banner state) {
        // copies the list first
        state.setPatterns(patterns);
    }

}
