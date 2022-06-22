package dudejoe870.chunkentity;

import dudejoe870.chunkentity.client.*;
import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.api.*;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.*;

public class ChunkEntity extends Entity {
    private static final ReferenceArraySet<ChunkEntity> loadedChunkEntities = new ReferenceArraySet<>();

    public static ReferenceSet<ChunkEntity> getAllLoadedChunkEntities() {
        return loadedChunkEntities;
    }

    @Environment(EnvType.CLIENT)
    protected static ChunkEntityModelBuilder defaultModelBuilder;

    private final ChunkEntityView<ChunkEntity> worldView = new ChunkEntityView<>(this);

    private int sizeX, sizeY, sizeZ;

    private PalettedContainer<BlockState> blockStates;

    public ChunkEntity(EntityType<? extends ChunkEntity> type, World world) {
        super(type, world);

        this.setSizeX(getInitialSizeX());
        this.setSizeY(getInitialSizeY());
        this.setSizeZ(getInitialSizeZ());

        this.blockStates = new PalettedContainer<>(
                Block.STATE_IDS,
                Blocks.AIR.getDefaultState(),
                PalettedContainer.PaletteProvider.BLOCK_STATE);
        if (world.isClient() && getModelBuilder() == null)
            initializeModelBuilder();

        loadedChunkEntities.add(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        loadedChunkEntities.remove(this);
    }

    public ChunkEntityView getWorldView() {
        return worldView;
    }

    public ChunkEntityModelBuilder getModelBuilder() {
        return defaultModelBuilder;
    }

    protected void initializeModelBuilder() {
        defaultModelBuilder = new ChunkEntityModelBuilder();
    }

    public void resizeChunk(int sizeX, int sizeY, int sizeZ) {
        this.setSizeX(sizeX);
        this.setSizeY(sizeY);
        this.setSizeZ(sizeZ);
        this.refreshPosition();

        if (!world.isClient()) ChunkEntityNetworking.RESIZE_S2C_HANDLER.sendAll(
                new ChunkEntityNetworking.ChunkEntityResizePacketS2C(this, this.getSizeX(), this.getSizeY(), this.getSizeZ()));
        else getModelBuilder().rebuild(this);
    }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    protected void setSizeX(int sizeX) { this.sizeX = sizeX; }
    protected void setSizeY(int sizeY) { this.sizeY = sizeY; }
    protected void setSizeZ(int sizeZ) { this.sizeZ = sizeZ; }

    protected boolean doesDynamicallyResize() {
        return true;
    }

    public boolean doesRotate() {
        return true;
    }

    ///*
    protected PalettedContainer<BlockState> getInitialState() {
        PalettedContainer<BlockState> state = new PalettedContainer<>(
                Block.STATE_IDS,
                Blocks.AIR.getDefaultState(),
                PalettedContainer.PaletteProvider.BLOCK_STATE);
        state.swap(0, 0, 0, Blocks.DIRT.getDefaultState());
        return state;
    }
    //*/

    // (For testing)
    /*
    protected PalettedContainer<BlockState> getInitialState() {
        PalettedContainer<BlockState> state = new PalettedContainer<>(
                Block.STATE_IDS,
                Blocks.AIR.getDefaultState(),
                PalettedContainer.PaletteProvider.BLOCK_STATE);

        try {
            state.lock();
            state.swapUnsafe(0, 0, 0, Blocks.AMETHYST_BLOCK.getDefaultState());
            state.swapUnsafe(1, 0, 0, Blocks.DEEPSLATE.getDefaultState());
            state.swapUnsafe(2, 0, 0, Blocks.GRASS_BLOCK.getDefaultState());
            state.swapUnsafe(2, 1, 0, Blocks.GRASS.getDefaultState());
            return state;
        } finally {
            state.unlock();
        }
    }
    */

    protected int getInitialSizeX() { return 1; }
    protected int getInitialSizeY() { return 1; }

    // (For testing)
    //protected int getInitialSizeX() { return 3; }
    //protected int getInitialSizeY() { return 2; }

    protected int getInitialSizeZ() { return 1; }

    /*
     Note: Doesn't rebuild the mesh automatically.
    */
    public void setBlockStates(PalettedContainer<BlockState> blockStates) {
        this.blockStates = blockStates;
    }

    public PalettedContainer<BlockState> getBlockStates() {
        return this.blockStates;
    }

    public void setBlockState(int x, int y, int z, BlockState blockState) {
        if (this.doesDynamicallyResize()) {
            int newSizeX = Math.max(this.getSizeX(), x+1);
            int newSizeY = Math.max(this.getSizeY(), y+1);
            int newSizeZ = Math.max(this.getSizeZ(), z+1);
            this.resizeChunk(newSizeX, newSizeY, newSizeZ);
        } else {
            if (x >= this.getSizeX() || y >= this.getSizeY() || z >= this.getSizeZ()) return;
        }

        blockStates.swap(x, y, z, blockState);

        if (!world.isClient()) {
            ChunkEntityNetworking.BLOCK_SET_S2C_HANDLER.sendAll(
                    new ChunkEntityNetworking.ChunkEntityBlockSetPacketS2C(this, x, y, z, blockState));
        }
        else if (world.isClient()) getModelBuilder().rebuild(this);
    }

    public BlockState getBlockState(int x, int y, int z) {
        if ((x >= this.getSizeX() || y >= this.getSizeY() || z >= this.getSizeZ()) || (x < 0 || y < 0 || z < 0))
            return Blocks.AIR.getDefaultState();
        return blockStates.get(x, y, z);
    }

    @Override
    protected Box calculateBoundingBox() {
        return new Box(
                this.getX(), this.getY(), this.getZ(),
                this.getX() + this.getSizeX(), this.getY() + this.getSizeY(), this.getZ() + this.getSizeZ());
    }

    @Override
    public Box getVisibilityBoundingBox() {
        if (this.doesRotate()) {
            // Make sure there's enough room for it to rotate if it needs to.
            int xzPlaneSize = Math.max(this.getSizeX() * 2, this.getSizeZ() * 2);
            int yHeight = Math.max(this.getSizeY() * 2, xzPlaneSize);

            return new Box(
                    this.getX() - (xzPlaneSize / 2) - 1, this.getY() - (yHeight / 2) - 1, this.getZ() - (xzPlaneSize / 2) - 1,
                    this.getX() + (xzPlaneSize / 2) + 1, this.getY() + (yHeight / 2) + 1, this.getZ() + (xzPlaneSize / 2) + 1);
        }
        else return this.calculateBoundingBox();
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        int sizeX = nbt.contains("chunk_size_x") ? nbt.getInt("chunk_size_x") : getInitialSizeX();
        int sizeY = nbt.contains("chunk_size_y") ? nbt.getInt("chunk_size_y") : getInitialSizeY();
        int sizeZ = nbt.contains("chunk_size_z") ? nbt.getInt("chunk_size_z") : getInitialSizeZ();
        this.resizeChunk(sizeX, sizeY, sizeZ);

        if (nbt.contains("chunk_blockstates", NbtElement.COMPOUND_TYPE)) {
            NbtCompound compound = nbt.getCompound("chunk_blockstates");

            blockStates = ChunkEntityConstants.BLOCKSTATES_CODEC.parse(NbtOps.INSTANCE, compound).getOrThrow(false, ChunkEntityMod.LOGGER::error);
        }
        else blockStates = getInitialState();

        if (!world.isClient()) {
            ChunkEntityNetworking.CLIENT_RESYNC_S2C_HANDLER.sendAll(
                    new ChunkEntityNetworking.ChunkEntityClientResyncPacketS2C(
                            this,
                            blockStates,
                            this.getSizeX(), this.getSizeY(), this.getSizeZ()));
        }
        else getModelBuilder().rebuild(this);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("chunk_size_x", this.getSizeX());
        nbt.putInt("chunk_size_y", this.getSizeY());
        nbt.putInt("chunk_size_z", this.getSizeZ());

        nbt.put("chunk_blockstates", ChunkEntityConstants.BLOCKSTATES_CODEC.encodeStart(NbtOps.INSTANCE, blockStates)
                .getOrThrow(false, ChunkEntityMod.LOGGER::error));
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        ChunkEntityNetworking.CLIENT_READY_FOR_RESYNC_C2S_HANDLER.send(
                new ChunkEntityNetworking.ChunkEntityClientReadyForResyncPacketC2S(this));
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    @Override
    public boolean isInsideWall() {
        return false;
    }
}
