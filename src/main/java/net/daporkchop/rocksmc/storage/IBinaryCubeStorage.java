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

package net.daporkchop.rocksmc.storage;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.ChunkPos;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * @author DaPorkchop_
 */
public interface IBinaryCubeStorage extends ICubicStorage, Closeable {
    /**
     * Reads the raw binary data for multiple cubes+columns at once.
     *
     * @param positions a {@link PosBatch} containing the positions of all the cubes+columns to read
     * @return a {@link BinaryBatch} containing all the given cube+column positions mapped to their corresponding NBT data, or {@code null} for cubes/columns that can't be found
     */
    BinaryBatch readBinaryBatch(PosBatch positions) throws IOException;

    /**
     * Writes the raw binary data for multiple cubes+columns at once.
     *
     * @param batch a {@link BinaryBatch} containing the cube+column positions and the NBT data to write to each
     */
    void writeBinaryBatch(BinaryBatch batch) throws IOException;

    @Override
    void close() throws IOException;

    /**
     * A group of position+NBT data pairs for both column and cube data.
     * <p>
     * Used for bulk I/O operations.
     *
     * @author DaPorkchop_
     */
    class BinaryBatch {
        public final Map<ChunkPos, ByteBuf> columns;
        public final Map<CubePos, ByteBuf> cubes;

        public BinaryBatch(Map<ChunkPos, ByteBuf> columns, Map<CubePos, ByteBuf> cubes) {
            this.columns = Objects.requireNonNull(columns, "columns");
            this.cubes = Objects.requireNonNull(cubes, "cubes");
        }
    }
}
