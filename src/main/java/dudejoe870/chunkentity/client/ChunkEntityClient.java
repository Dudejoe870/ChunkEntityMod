package dudejoe870.chunkentity.client;

import dudejoe870.chunkentity.*;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public class ChunkEntityClient implements ClientModInitializer {
    @Nullable
    private ChunkEntity surroundingChunkEntity = null;

    /*
     Returns the Chunk Entity that this client is currently in (returns null if it isn't currently in a Chunk Entity)
    */
    @Nullable
    public ChunkEntity getSurroundingChunkEntity() {
        return surroundingChunkEntity;
    }

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ChunkEntityMod.CHUNK_ENTITY, ChunkEntityRenderer<ChunkEntity>::new);

        ChunkEntityNetworking.ClientInit();

        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if (client.getCameraEntity() != null) {
                surroundingChunkEntity = ChunkEntityUtil.getChunkEntityAtPosition(client.getCameraEntity().getEyePos());
            }
        });
    }
}
