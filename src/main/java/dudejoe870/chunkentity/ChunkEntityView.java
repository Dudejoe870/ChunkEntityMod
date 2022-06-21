package dudejoe870.chunkentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ChunkEntityView<E extends ChunkEntity> implements BlockRenderView {
    private final E entity;

    public ChunkEntityView(E entity) {
        this.entity = entity;
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        if (!shaded) return 1.0f;

        switch (direction) {
            case DOWN: {
                return 0.5f;
            }
            case UP: {
                return 1.0f;
            }
            case NORTH:
            case SOUTH: {
                return 0.8f;
            }
            case WEST:
            case EAST: {
                return 0.6f;
            }
        }
        return 1.0f;
    }

    @Override
    public LightingProvider getLightingProvider() {
        return null;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        return colorResolver.getColor(BuiltinRegistries.BIOME.get(new Identifier("plains")), pos.getX(), pos.getZ());
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        return type.value;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
        return 15 - ambientDarkness;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return BlockRenderView.super.getBlockEntity(pos, type);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return entity.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return null;
    }

    @Override
    public int getHeight() {
        return entity.getSizeY();
    }

    @Override
    public int getBottomY() {
        return 0;
    }
}
