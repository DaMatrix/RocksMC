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
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.util.Utils;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicColumnData;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class RocksLocalCubicWriter implements ChunkDataWriter<RocksLocalCubicColumnData> {
    @NonNull
    private final Path dstPath;
    private final Map<Dimension, IBinaryCubeStorage> saves = new ConcurrentHashMap<>();

    @Override
    public void accept(RocksLocalCubicColumnData data) throws IOException {
        IBinaryCubeStorage save = this.saves.computeIfAbsent(data.dimension(), dim -> {
            try {
                return new LocalStorageImpl(null, this.dstPath.resolve(dim.getDirectory()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        int x = data.position().getEntryX();
        int z = data.position().getEntryZ();
        save.writeBinaryBatch(new IBinaryCubeStorage.BinaryBatch(
                data.columnData() == null ? Collections.emptyMap() : Collections.singletonMap(new ChunkPos(x, z), Unpooled.wrappedBuffer(data.columnData())),
                data.cubeData().entrySet().stream().collect(Collectors.toMap(e -> new CubePos(x, e.getKey(), z), e -> Unpooled.wrappedBuffer(e.getValue())))));
    }

    @Override
    public void discardData() throws IOException {
        Utils.rm(this.dstPath);
    }

    @Override
    public void close() throws Exception {
        boolean exception = false;
        for (IBinaryCubeStorage save : this.saves.values()) {
            try {
                save.close();
            } catch (IOException e) {
                e.printStackTrace();
                exception = true;
            }
        }
        if (exception) {
            throw new IOException();
        }
    }
}
