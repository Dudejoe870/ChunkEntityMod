package dudejoe870.chunkentity.mixin.client;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(WorldRenderer.class)
public interface WorldRendererInvoker {
    @Invoker("drawCuboidShapeOutline")
    public static void invokeDrawCuboidShapeOutline(
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            VoxelShape shape,
            double offsetX, double offsetY, double offsetZ,
            float red, float green, float blue, float alpha) {
        throw new AssertionError();
    }
}
