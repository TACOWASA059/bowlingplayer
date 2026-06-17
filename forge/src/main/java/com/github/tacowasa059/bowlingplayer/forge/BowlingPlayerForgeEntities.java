package com.github.tacowasa059.bowlingplayer.forge;

import com.github.tacowasa059.bowlingplayer.BowlingPlayerConstants;
import com.github.tacowasa059.bowlingplayer.entity.BowlingDisplayEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BowlingPlayerForgeEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BowlingPlayerConstants.MOD_ID);

    public static final RegistryObject<EntityType<BowlingDisplayEntity>> BOWLING_DISPLAY = ENTITY_TYPES.register(
            "bowling_display",
            () -> EntityType.Builder.<BowlingDisplayEntity>of(BowlingDisplayEntity::new, MobCategory.MISC)
                    .sized(BowlingDisplayEntity.DEFAULT_PIN_WIDTH, BowlingDisplayEntity.DEFAULT_PIN_HEIGHT)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build(BowlingPlayerConstants.MOD_ID + ":bowling_display")
    );

    private BowlingPlayerForgeEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
