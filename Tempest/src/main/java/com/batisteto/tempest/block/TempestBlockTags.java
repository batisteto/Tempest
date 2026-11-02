package com.batisteto.tempest.block;

import com.batisteto.tempest.Tempest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Which blocks a tornado of a given destruction tier is allowed to rip up.
 * Server owners can extend/replace these via a datapack tag override.
 */
public final class TempestBlockTags {
    public static final TagKey<Block> TIER_0 = tag("tornado_destructible_tier0");
    public static final TagKey<Block> TIER_1 = tag("tornado_destructible_tier1");
    public static final TagKey<Block> TIER_2 = tag("tornado_destructible_tier2");
    public static final TagKey<Block> TIER_3 = tag("tornado_destructible_tier3");

    private TempestBlockTags() {
    }

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Tempest.MODID, name));
    }

    public static TagKey<Block> destructibleTagFor(int tier) {
        return switch (Math.max(0, Math.min(3, tier))) {
            case 0 -> TIER_0;
            case 1 -> TIER_1;
            case 2 -> TIER_2;
            default -> TIER_3;
        };
    }
}
