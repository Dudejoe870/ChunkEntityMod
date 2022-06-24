package dudejoe870.chunkentity;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.PalettedContainer;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

public class ChunkEntityNetworking {
    public static abstract class ChunkEntityPacket {
        public ChunkEntity entity;

        public static abstract class Handler<T extends ChunkEntityPacket> {
            public static abstract class S2C<T extends ChunkEntityPacket> extends Handler<T> {
                public S2C() {
                    ClientPlayNetworking.registerGlobalReceiver(getPacketId(), (client, handler, buffer, sender) -> {
                        T packet = read(client.world, buffer);

                        client.execute(() -> {
                            handle(client, handler, packet, sender);
                        });
                    });

                    ChunkEntityMod.LOGGER.info(String.format("Client-side packet handler registered for %s",
                            ((Class<T>) ((ParameterizedType) getClass()
                                    .getGenericSuperclass()).getActualTypeArguments()[0]).getTypeName()));
                }

                public void send(T packet, ServerPlayerEntity player) {
                    PacketByteBuf buffer = PacketByteBufs.create();
                    write(buffer, packet);

                    ServerPlayNetworking.send(player, getPacketId(), buffer);
                }

                public void sendAll(T packet) {
                    PacketByteBuf buffer = PacketByteBufs.create();
                    write(buffer, packet);

                    for (ServerPlayerEntity player : PlayerLookup.tracking(packet.entity))
                        ServerPlayNetworking.send(player, getPacketId(), buffer);
                }

                public abstract void handle(MinecraftClient client, ClientPlayNetworkHandler handler, T packet, PacketSender sender);
            }

            public static abstract class C2S<T extends ChunkEntityPacket> extends Handler<T> {
                public C2S() {
                    ServerPlayNetworking.registerGlobalReceiver(getPacketId(), (server, player, handler, buffer, sender) -> {
                        T packet = read(player.world, buffer);

                        server.execute(() -> {
                            handle(server, player, handler, packet, sender);
                        });
                    });

                    ChunkEntityMod.LOGGER.info(String.format("Server-side packet handler registered for %s",
                            ((Class<T>) ((ParameterizedType) getClass()
                            .getGenericSuperclass()).getActualTypeArguments()[0]).getTypeName()));
                }

                public void send(T packet) {
                    PacketByteBuf buffer = PacketByteBufs.create();
                    write(buffer, packet);

                    ClientPlayNetworking.send(getPacketId(), buffer);
                }

                public abstract void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, T packet, PacketSender sender);
            }

            public void write(PacketByteBuf buffer, T packet) {
                buffer.writeInt(packet.entity.getId());

                writeCustomData(buffer, packet);
            }

            public T read(World world, PacketByteBuf buffer) {
                T packet = createEmptyPacket();
                packet.entity = (ChunkEntity)world.getEntityById(buffer.readInt());

                readCustomData(buffer, packet);
                return packet;
            }

            protected abstract T createEmptyPacket();

            public abstract Identifier getPacketId();

            protected abstract void writeCustomData(PacketByteBuf buffer, T packet);
            protected abstract void readCustomData(PacketByteBuf buffer, T packet);
        }

        public ChunkEntityPacket() {
            this.entity = null;
        }

        public ChunkEntityPacket(ChunkEntity entity) {
            this.entity = entity;
        }
    }

    public static ChunkEntityBlockSetPacketS2C.Handler BLOCK_SET_S2C_HANDLER;
    public static ChunkEntityResizePacketS2C.Handler RESIZE_S2C_HANDLER;
    public static ChunkEntityClientResyncPacketS2C.Handler CLIENT_RESYNC_S2C_HANDLER;
    public static ChunkEntityClientReadyForResyncPacketC2S.Handler CLIENT_READY_FOR_RESYNC_C2S_HANDLER;

    public static void ClientInit() {
        BLOCK_SET_S2C_HANDLER = new ChunkEntityBlockSetPacketS2C.Handler();
        RESIZE_S2C_HANDLER = new ChunkEntityResizePacketS2C.Handler();
        CLIENT_RESYNC_S2C_HANDLER = new ChunkEntityClientResyncPacketS2C.Handler();
    }

    public static void ServerInit() {
        CLIENT_READY_FOR_RESYNC_C2S_HANDLER = new ChunkEntityClientReadyForResyncPacketC2S.Handler();
    }

    public static class ChunkEntityBlockSetPacketS2C extends ChunkEntityPacket {
        public int x, y, z;
        public BlockState blockState;

        public static class Handler extends ChunkEntityPacket.Handler.S2C<ChunkEntityBlockSetPacketS2C> {
            @Override
            protected ChunkEntityBlockSetPacketS2C createEmptyPacket() {
                return new ChunkEntityBlockSetPacketS2C();
            }

            @Override
            public Identifier getPacketId() {
                return new Identifier(ChunkEntityConstants.MOD_ID, "chunkentity_blockset");
            }

            @Override
            protected void writeCustomData(PacketByteBuf buffer, ChunkEntityBlockSetPacketS2C packet) {
                buffer.writeInt(packet.x);
                buffer.writeInt(packet.y);
                buffer.writeInt(packet.z);

                buffer.writeIdentifier(Registry.BLOCK.getId(packet.blockState.getBlock()));
            }

            @Override
            protected void readCustomData(PacketByteBuf buffer, ChunkEntityBlockSetPacketS2C packet) {
                packet.x = buffer.readInt();
                packet.y = buffer.readInt();
                packet.z = buffer.readInt();

                packet.blockState = Registry.BLOCK.get(buffer.readIdentifier()).getDefaultState();
            }

            @Override
            public void handle(MinecraftClient client, ClientPlayNetworkHandler handler, ChunkEntityBlockSetPacketS2C packet, PacketSender sender) {
                packet.entity.setBlockState(packet.x, packet.y, packet.z, packet.blockState);
            }
        }

        public ChunkEntityBlockSetPacketS2C() {
            super();
        }

        public ChunkEntityBlockSetPacketS2C(ChunkEntity entity, int x, int y, int z, BlockState blockState) {
            super(entity);

            this.x = x;
            this.y = y;
            this.z = z;

            this.blockState = blockState;
        }
    }

    public static class ChunkEntityResizePacketS2C extends ChunkEntityPacket {
        public int sizeX, sizeY, sizeZ;

        public static class Handler extends ChunkEntityPacket.Handler.S2C<ChunkEntityResizePacketS2C> {
            @Override
            protected ChunkEntityResizePacketS2C createEmptyPacket() {
                return new ChunkEntityResizePacketS2C();
            }

            @Override
            public Identifier getPacketId() {
                return new Identifier(ChunkEntityConstants.MOD_ID, "chunkentity_resize");
            }

            @Override
            protected void writeCustomData(PacketByteBuf buffer, ChunkEntityResizePacketS2C packet) {
                buffer.writeInt(packet.sizeX);
                buffer.writeInt(packet.sizeY);
                buffer.writeInt(packet.sizeZ);
            }

            @Override
            protected void readCustomData(PacketByteBuf buffer, ChunkEntityResizePacketS2C packet) {
                packet.sizeX = buffer.readInt();
                packet.sizeY = buffer.readInt();
                packet.sizeZ = buffer.readInt();
            }

            @Override
            public void handle(MinecraftClient client, ClientPlayNetworkHandler handler, ChunkEntityResizePacketS2C packet, PacketSender sender) {
                packet.entity.resizeChunk(packet.sizeX, packet.sizeY, packet.sizeZ);
            }
        }

        public ChunkEntityResizePacketS2C() {
            super();
        }

        public ChunkEntityResizePacketS2C(ChunkEntity entity, int sizeX, int sizeY, int sizeZ) {
            super(entity);

            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }
    }

    public static class ChunkEntityClientResyncPacketS2C extends ChunkEntityPacket {
        public PalettedContainer<BlockState> blockStates;

        public int sizeX, sizeY, sizeZ;

        public static class Handler extends ChunkEntityPacket.Handler.S2C<ChunkEntityClientResyncPacketS2C> {
            @Override
            protected ChunkEntityClientResyncPacketS2C createEmptyPacket() {
                return new ChunkEntityClientResyncPacketS2C();
            }

            @Override
            public Identifier getPacketId() {
                return new Identifier(ChunkEntityConstants.MOD_ID, "chunkentity_resync");
            }

            @Override
            protected void writeCustomData(PacketByteBuf buffer, ChunkEntityClientResyncPacketS2C packet) {
                packet.blockStates.writePacket(buffer);

                buffer.writeInt(packet.sizeX);
                buffer.writeInt(packet.sizeY);
                buffer.writeInt(packet.sizeZ);
            }

            @Override
            protected void readCustomData(PacketByteBuf buffer, ChunkEntityClientResyncPacketS2C packet) {
                packet.blockStates.readPacket(buffer);

                packet.sizeX = buffer.readInt();
                packet.sizeY = buffer.readInt();
                packet.sizeZ = buffer.readInt();
            }

            @Override
            public void handle(MinecraftClient client, ClientPlayNetworkHandler handler, ChunkEntityClientResyncPacketS2C packet, PacketSender sender) {
                ChunkEntityMod.LOGGER.info(String.format("Resyncing Chunk Entity (id=%d)", packet.entity.getId()));

                packet.entity.setBlockStates(packet.blockStates);
                packet.entity.resizeChunk(packet.sizeX, packet.sizeY, packet.sizeZ);
            }
        }

        public ChunkEntityClientResyncPacketS2C() {
            super();
            this.blockStates = new PalettedContainer<>(
                    Block.STATE_IDS,
                    Blocks.AIR.getDefaultState(),
                    PalettedContainer.PaletteProvider.BLOCK_STATE);
        }

        public ChunkEntityClientResyncPacketS2C(
                ChunkEntity entity,
                PalettedContainer<BlockState> blockStates,
                int sizeX, int sizeY, int sizeZ) {
            super(entity);
            this.blockStates = blockStates;

            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }
    }

    public static class ChunkEntityClientReadyForResyncPacketC2S extends ChunkEntityPacket {
        public static class Handler extends ChunkEntityPacket.Handler.C2S<ChunkEntityClientReadyForResyncPacketC2S> {
            @Override
            protected ChunkEntityClientReadyForResyncPacketC2S createEmptyPacket() {
                return new ChunkEntityClientReadyForResyncPacketC2S();
            }

            @Override
            public Identifier getPacketId() {
                return new Identifier(ChunkEntityConstants.MOD_ID, "chunkentity_ready_for_resync");
            }

            @Override
            protected void writeCustomData(PacketByteBuf buffer, ChunkEntityClientReadyForResyncPacketC2S packet) {
            }

            @Override
            protected void readCustomData(PacketByteBuf buffer, ChunkEntityClientReadyForResyncPacketC2S packet) {
            }

            // TODO: Add a timeout per player (this could be a potential attack vector for DDOS attacks on servers)
            @Override
            public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, ChunkEntityClientReadyForResyncPacketC2S packet, PacketSender sender) {
                ChunkEntityMod.LOGGER.info(String.format("Player %s (uuid=%s) requested resync for Chunk Entity (id=%d)",
                        player.getDisplayName().getString(), player.getUuidAsString(), packet.entity.getId()));

                CLIENT_RESYNC_S2C_HANDLER.send(
                        new ChunkEntityClientResyncPacketS2C(packet.entity,
                                packet.entity.getBlockStates(),
                                packet.entity.getSizeX(), packet.entity.getSizeY(), packet.entity.getSizeZ()), player);
            }
        }

        public ChunkEntityClientReadyForResyncPacketC2S() {
            super();
        }

        public ChunkEntityClientReadyForResyncPacketC2S(ChunkEntity entity) {
            super(entity);
        }
    }
}
