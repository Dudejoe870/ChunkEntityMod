package dudejoe870.chunkentity.client;

import com.google.common.collect.Queues;
import dudejoe870.chunkentity.ChunkEntity;
import dudejoe870.chunkentity.ChunkEntityMod;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.fabricmc.api.*;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.thread.TaskExecutor;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ChunkEntityModelBuilder<E extends ChunkEntity> {
    public class Task {
        private final E entity;

        private final AtomicBoolean cancelled = new AtomicBoolean();

        public Map<RenderLayer, VertexBuffer> vertexBuffers =
                RenderLayer.getBlockLayers().stream().collect(
                        Collectors.toMap(Function.identity(), renderLayer -> new VertexBuffer()));

        public BlockBufferBuilderStorage builders = new BlockBufferBuilderStorage();

        public Task(E entity) {
            this.entity = entity;
        }

        public final void cancel() {
            cancelled.set(true);
            ChunkEntityMod.LOGGER.info(String.format("Cancelling build task for Chunk Entity (id=%d)", entity.getId()));
        }

        public final void reset() {
            cancelled.set(false);
        }

        public final boolean isCancelled() {
            return cancelled.get();
        }

        protected void build() {
            if (cancelled.get()) {
                ChunkEntityMod.LOGGER.info(
                        String.format("Cancelled before starting rebuild for Chunk Entity (id=%d)", entity.getId()));
                return;
            }

            ChunkEntityMod.LOGGER.info(
                    String.format("Rebuilding Chunk Entity (id=%d)", entity.getId()));

            MatrixStack matrices = new MatrixStack();

            BlockPos startPos = new BlockPos(0, 0, 0);
            BlockPos endPos = new BlockPos(entity.getSizeX()-1, entity.getSizeY()-1, entity.getSizeZ()-1);

            ReferenceArraySet<RenderLayer> blockLayers = new ReferenceArraySet<>(RenderLayer.getBlockLayers().size());

            if (cancelled.get()) {
                ChunkEntityMod.LOGGER.info(
                        String.format("Cancelled rebuild for Chunk Entity (id=%d)", entity.getId()));
                return;
            }

            for (BlockPos blockPos : BlockPos.iterate(startPos, endPos)) {
                BlockState blockState = entity.getBlockState(blockPos.getX(), blockPos.getY(), blockPos.getZ());

                if (blockState.hasBlockEntity()) continue; // TODO: Implement Block Entities.

                if (blockState.getRenderType() == BlockRenderType.INVISIBLE) continue;

                RenderLayer blockLayer = RenderLayers.getBlockLayer(blockState);
                BufferBuilder builder = builders.get(blockLayer);
                if (blockLayers.add(blockLayer))
                    builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                {
                    Random random = Random.create();

                    BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();

                    matrices.push();
                    {
                        matrices.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                        blockRenderManager.renderBlock(blockState, blockPos, entity.getWorldView(), matrices, builder, true, random);
                    }
                    matrices.pop();
                }

                if (cancelled.get()) {
                    ChunkEntityMod.LOGGER.info(
                            String.format("Cancelled rebuild for Chunk Entity (id=%d)", entity.getId()));
                    return;
                }
            }

            ArrayList<CompletableFuture<Void>> uploads = new ArrayList<>();
            for (RenderLayer layer : blockLayers) {
                BufferBuilder.BuiltBuffer builtBuffer = builders.get(layer).end();
                uploads.add(scheduleUpload(builtBuffer, vertexBuffers.get(layer)));
            }
            for (CompletableFuture<Void> upload : uploads)
                upload.join();

            if (cancelled.get()) {
                ChunkEntityMod.LOGGER.info(
                        String.format("Cancelled rebuild for Chunk Entity (id=%d) after finishing", entity.getId()));
                return;
            }
            ChunkEntityMod.LOGGER.info(
                    String.format("Finished rebuild for Chunk Entity (id=%d)", entity.getId()));
        }
    }

    private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();

    protected Queue<Task> queuedTasks = Queues.newLinkedBlockingDeque();
    private final Map<Task, CompletableFuture<Void>> runningTasks = new Reference2ObjectArrayMap<>();

    private final TaskExecutor<Runnable> mailbox;

    private final Map<E, Task> tasks = new Reference2ObjectArrayMap<>();

    public ChunkEntityModelBuilder() {
        this.mailbox = TaskExecutor.create(Util.getMainWorkerExecutor(), getName());
        this.mailbox.send(this::scheduleRunTasks);
    }

    public Task getTaskForEntity(E entity) {
        return tasks.get(entity);
    }

    protected String getName() {
        return "Chunk Entity ModelBuilder";
    }

    private void scheduleRunTasks() {
        Task task = queuedTasks.poll();
        if (task == null) return;
        runningTasks.put(task,
                CompletableFuture.runAsync(task::build,
                        Util.getMainWorkerExecutor()).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create(throwable, "Chunk Entity batching"));
                        return;
                    }
                    this.mailbox.send(() -> {
                        if (task.isCancelled()) task.builders.reset();
                        else {
                            task.builders.clear();
                            task.reset();
                        }
                        this.scheduleRunTasks();
                    });
                }));
    }

    public final void uploadVertexBuffers() {
        Runnable runnable;
        while ((runnable = this.uploadQueue.poll()) != null)
            runnable.run();
    }

    protected final void sendTask(Task task) {
        this.queuedTasks.remove(task);
        CompletableFuture<Void> runningFuture = runningTasks.get(task);
        if (runningFuture != null) {
            task.cancel();
            runningFuture.join();
        }
        this.mailbox.send(() -> {
            this.queuedTasks.offer(task);
            this.scheduleRunTasks();
        });
    }

    protected final CompletableFuture<Void> scheduleUpload(BufferBuilder.BuiltBuffer builtBuffer, VertexBuffer glBuffer) {
        return CompletableFuture.runAsync(() -> {
            if (glBuffer.isClosed()) {
                return;
            }
            glBuffer.bind();
            glBuffer.upload(builtBuffer);
            VertexBuffer.unbind();
        }, this.uploadQueue::add);
    }

    public void rebuild(E chunkEntity) {
        ChunkEntityMod.LOGGER.info(
                String.format("Scheduling rebuild for Chunk Entity (id=%d)", chunkEntity.getId()));
        if (!tasks.containsKey(chunkEntity))
            tasks.put(chunkEntity, new Task(chunkEntity));
        this.sendTask(getTaskForEntity(chunkEntity));
    }
}
