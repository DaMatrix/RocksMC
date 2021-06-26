/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2021-2021 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.rocksmc.converter.io;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.io.BaseMinecraftReader;
import cubicchunks.converter.lib.util.UncheckedInterruptedException;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.util.CheckedFunction;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.NonNull;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicColumnData;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;
import net.daporkchop.rocksmc.util.NBTSerializerUtils;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
public class RocksLocalCubicReader extends BaseMinecraftReader<RocksLocalCubicColumnData, IBinaryCubeStorage> {
    private static Path getDimensionPath(Dimension d, Path worldDir) {
        if (!d.getDirectory().isEmpty()) {
            worldDir = worldDir.resolve(d.getDirectory());
        }
        return worldDir;
    }

    private final CompletableFuture<Map<Dimension, Map<ChunkPos, IntList>>> chunkList = new CompletableFuture<>();
    private final Thread loadThread = Thread.currentThread();

    public RocksLocalCubicReader(@NonNull Path srcDir) {
        super(srcDir, (dim, path) -> Files.exists(getDimensionPath(dim, path))
                ? Utils.propagateExceptions((CheckedFunction<Path, IBinaryCubeStorage, IOException>) p -> new LocalStorageImpl(null, p)).apply(getDimensionPath(dim, path))
                : null);
    }

    @Override
    public void countInputChunks(@NonNull Runnable increment) throws IOException, InterruptedException {
        //adapted from CubicChunkReader
        try {
            Map<Dimension, Map<ChunkPos, IntList>> dimensions = new Object2ObjectOpenHashMap<>();

            for (Map.Entry<Dimension, IBinaryCubeStorage> entry : this.saves.entrySet()) {
                Map<ChunkPos, IntList> chunksMap = new HashMap<>();
                dimensions.put(entry.getKey(), chunksMap);

                entry.getValue().forEachCube(cubePos -> chunksMap.computeIfAbsent(cubePos.chunkPos(), chunkPos -> {
                    increment.run();
                    return new IntArrayList();
                }).add(cubePos.getY()));
            }
            this.chunkList.complete(dimensions);
        } catch (UncheckedInterruptedException ignored) {
            this.chunkList.complete(null);
        }
    }

    @Override
    public void loadChunks(@NonNull Consumer<? super RocksLocalCubicColumnData> consumer, @NonNull Predicate<Throwable> errorHandler) throws IOException, InterruptedException {
        try {
            Map<Dimension, Map<ChunkPos, IntList>> chunksByDimension = this.chunkList.get();
            if (chunksByDimension == null) {
                return; //counting interrupted
            }

            for (Map.Entry<Dimension, Map<ChunkPos, IntList>> dimEntry : chunksByDimension.entrySet()) {
                if (Thread.interrupted()) {
                    return;
                }

                Dimension dim = dimEntry.getKey();
                IBinaryCubeStorage storage = this.saves.get(dim);
                dimEntry.getValue().entrySet().parallelStream().forEach(chunkEntry -> {
                    if (Thread.interrupted()) {
                        return;
                    }

                    ChunkPos chunkPos = chunkEntry.getKey();
                    IntList cubes = chunkEntry.getValue();

                    IBinaryCubeStorage.BinaryBatch batch;
                    try {
                        batch = storage.readBinaryBatch(new ICubicStorage.PosBatch(
                                Collections.singleton(chunkPos),
                                cubes.stream().map(y -> new CubePos(chunkPos.x, y, chunkPos.z)).collect(Collectors.toSet())));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return; //interrupted?
                    }

                    consumer.accept(new RocksLocalCubicColumnData(dim, new EntryLocation2D(chunkPos.x, chunkPos.z),
                            NBTSerializerUtils.toHeapNioBuffer(batch.columns.values().iterator().next()),
                            batch.cubes.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getY(), e -> NBTSerializerUtils.toHeapNioBuffer(e.getValue())))));
                });
            }
        } catch (UncheckedInterruptedException ex) {
            // interrupted, do nothing
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        this.loadThread.interrupt();
    }
}
