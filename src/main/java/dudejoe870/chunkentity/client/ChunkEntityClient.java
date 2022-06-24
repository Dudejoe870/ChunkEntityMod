package dudejoe870.chunkentity.client;

import dudejoe870.chunkentity.*;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public class ChunkEntityClient implements ClientModInitializer {
    @Nullable
    private ChunkEntityUtil.ChunkEntityIntersectionInfo chunkEntityTarget = null;

    /*
     Returns the Chunk Entity that this client is currently facing and can reach
     (returns null if it there isn't one in reach)
    */
    @Nullable
    public ChunkEntityUtil.ChunkEntityIntersectionInfo getChunkEntityTarget() {
        return chunkEntityTarget;
    }

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ChunkEntityMod.CHUNK_ENTITY, ChunkEntityRenderer<ChunkEntity>::new);

        ChunkEntityNetworking.ClientInit();

        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if (client.interactionManager != null) {
                Camera camera = client.gameRenderer.getCamera();

                float f = camera.getPitch() * ((float)Math.PI / 180);
                float g = -camera.getYaw() * ((float)Math.PI / 180);
                float h = MathHelper.cos(g);
                float i = MathHelper.sin(g);
                float j = MathHelper.cos(f);
                float k = MathHelper.sin(f);
                Vec3d rotationVec = new Vec3d(i * j, -k, h * j);

                chunkEntityTarget =
                        ChunkEntityUtil.getChunkEntityAlongRay(camera.getPos(), rotationVec);
            }
        });
    }
}
