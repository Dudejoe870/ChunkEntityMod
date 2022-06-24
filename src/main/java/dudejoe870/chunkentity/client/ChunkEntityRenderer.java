package dudejoe870.chunkentity.client;

import com.mojang.blaze3d.systems.*;
import dudejoe870.chunkentity.*;
import dudejoe870.chunkentity.mixin.client.WorldRendererInvoker;
import net.fabricmc.api.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.*;

@Environment(EnvType.CLIENT)
public class ChunkEntityRenderer<E extends ChunkEntity> extends EntityRenderer<E> {
    protected ChunkEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(E entity) {
        return null;
    }

    @Override
    public void render(E chunkEntity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light) {
        chunkEntity.getModelBuilder().uploadVertexBuffers();

        ChunkEntityModelBuilder.Task task = chunkEntity.getModelBuilder().getTaskForEntity(chunkEntity);
        if (task == null) return;

        if (chunkEntity.doesRotate()) {
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0f - yaw));
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(chunkEntity.getPitch()));
        }

        Profiler profiler = chunkEntity.world.getProfiler();
        profiler.push("chunk_entity_rendering");
        {
            profiler.swap("terrain");
            renderLayer(RenderLayer.getSolid(), matrices, chunkEntity);
            renderLayer(RenderLayer.getCutoutMipped(), matrices, chunkEntity);
            renderLayer(RenderLayer.getCutout(), matrices, chunkEntity);

            // (For testing)
            /*
            profiler.swap("block_outline");
            drawBlockOutline(matrices, new BlockPos(0, 0, 0), chunkEntity,
                    vertexConsumerProvider.getBuffer(RenderLayer.getLines()));
            */
        }
        profiler.pop();
    }

    protected void drawBlockOutline(MatrixStack matrices, BlockPos pos, E chunkEntity, VertexConsumer consumer) {
        BlockState state = chunkEntity.getBlockState(pos);

        WorldRendererInvoker.invokeDrawCuboidShapeOutline(matrices, consumer,
                state.getOutlineShape(chunkEntity.getWorldView(), pos, ShapeContext.of(chunkEntity)),
                pos.getX(), pos.getY(), pos.getZ(),
                0.0f, 0.0f, 0.0f, 0.4f);
    }

    protected void renderLayer(RenderLayer layer, MatrixStack matrices, E chunkEntity) {
        if (layer == RenderLayer.getTranslucent()) return; // TODO: Add support for the translucent layer.

        RenderSystem.assertOnRenderThread();

        ChunkEntityModelBuilder.Task task = chunkEntity.getModelBuilder().getTaskForEntity(chunkEntity);
        if (task == null) return;

        VertexBuffer vertexBuffer = (VertexBuffer)task.vertexBuffers.get(layer);
        if (vertexBuffer.getVertexFormat() == null || vertexBuffer.isClosed()) return;

        Profiler profiler = chunkEntity.world.getProfiler();
        profiler.swap(() -> "render_layer_" + layer);
        layer.startDrawing();
        {
            Shader shader = RenderSystem.getShader();
            for (int i = 0; i < 12; ++i) {
                int texture = RenderSystem.getShaderTexture(i);
                shader.addSampler("Sampler" + i, texture);
            }
            if (shader.modelViewMat != null)
                shader.modelViewMat.set(matrices.peek().getPositionMatrix());
            if (shader.projectionMat != null)
                shader.projectionMat.set(RenderSystem.getProjectionMatrix());
            if (shader.colorModulator != null)
                shader.colorModulator.set(RenderSystem.getShaderColor());
            if (shader.fogStart != null)
                shader.fogStart.set(RenderSystem.getShaderFogStart());
            if (shader.fogEnd != null)
                shader.fogEnd.set(RenderSystem.getShaderFogEnd());
            if (shader.fogColor != null)
                shader.fogColor.set(RenderSystem.getShaderFogColor());
            if (shader.fogShape != null)
                shader.fogShape.set(RenderSystem.getShaderFogShape().getId());
            if (shader.textureMat != null)
                shader.textureMat.set(RenderSystem.getTextureMatrix());
            if (shader.gameTime != null)
                shader.gameTime.set(RenderSystem.getShaderGameTime());
            if (shader.chunkOffset != null)
                shader.chunkOffset.set(Vec3f.ZERO);
            if (shader.lineWidth != null)
                shader.lineWidth.set(RenderSystem.getShaderLineWidth());
            if (shader.screenSize != null) {
                Window window = MinecraftClient.getInstance().getWindow();
                shader.screenSize.set((float)window.getFramebufferWidth(), (float)window.getFramebufferHeight());
            }
            RenderSystem.setupShaderLights(shader);

            shader.bind();
            {
                vertexBuffer.bind();
                vertexBuffer.drawElements();
            }
            shader.unbind();
            VertexBuffer.unbind();
        }
        layer.endDrawing();
    }
}
