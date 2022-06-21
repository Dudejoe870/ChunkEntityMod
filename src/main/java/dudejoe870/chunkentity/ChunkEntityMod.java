package dudejoe870.chunkentity;

import net.fabricmc.api.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.*;
import net.minecraft.entity.*;
import org.slf4j.*;
import net.minecraft.util.registry.*;

public class ChunkEntityMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ChunkEntityConstants.MOD_NAME);

    public static final EntityType<ChunkEntity> CHUNK_ENTITY = Registry.register(
            Registry.ENTITY_TYPE,
            ChunkEntityConstants.CHUNK_ENTITY_ID,
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, ChunkEntity::new)
                    .dimensions(EntityDimensions.changing(1.0f, 1.0f))
                    .build()
    );

    @Override
    public void onInitialize() {
        ChunkEntityNetworking.ServerInit();
    }
}
