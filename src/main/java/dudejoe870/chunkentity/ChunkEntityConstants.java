package dudejoe870.chunkentity;

import com.mojang.serialization.*;
import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.world.chunk.*;

public class ChunkEntityConstants {
    // General
    public static final String MOD_ID = "chunkentity";
    public static final String MOD_NAME = "ChunkEntity";

    // Entities
    public static final Identifier CHUNK_ENTITY_ID = new Identifier(MOD_ID, "chunkentity");

    // Codecs
    public static final Codec<PalettedContainer<BlockState>> BLOCKSTATES_CODEC =
            PalettedContainer.method_44343(
                    Block.STATE_IDS,
                    BlockState.CODEC,
                    PalettedContainer.PaletteProvider.BLOCK_STATE,
                    Blocks.AIR.getDefaultState());
}
