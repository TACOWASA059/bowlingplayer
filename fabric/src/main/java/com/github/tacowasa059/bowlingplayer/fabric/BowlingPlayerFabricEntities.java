package com.github.tacowasa059.bowlingplayer.fabric;

import com.github.tacowasa059.bowlingplayer.BowlingPlayerConstants;
import com.github.tacowasa059.bowlingplayer.entity.BowlingDisplayEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class BowlingPlayerFabricEntities {
    public static final EntityType<BowlingDisplayEntity> BOWLING_DISPLAY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(BowlingPlayerConstants.MOD_ID, "bowling_display"),
            FabricEntityTypeBuilder.<BowlingDisplayEntity>create(MobCategory.MISC, BowlingDisplayEntity::new)
                    .dimensions(EntityDimensions.scalable(BowlingDisplayEntity.DEFAULT_PIN_WIDTH, BowlingDisplayEntity.DEFAULT_PIN_HEIGHT))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(20)
                    .build()
    );

    private BowlingPlayerFabricEntities() {
    }

    public static void init() {
    }
}
