package dudejoe870.chunkentity.client;

import dudejoe870.chunkentity.*;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;

@Environment(EnvType.CLIENT)
public class ChunkEntityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ChunkEntityMod.CHUNK_ENTITY, ChunkEntityRenderer<ChunkEntity>::new);

        ChunkEntityNetworking.ClientInit();
    }
}
