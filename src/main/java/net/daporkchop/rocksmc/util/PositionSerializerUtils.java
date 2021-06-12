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

package net.daporkchop.rocksmc.util;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;

import static java.lang.Math.*;

/**
 * Utilities for converting coordinates to and from their binary representation.
 * <p>
 * Coordinates are encoded with their bits interleaved, in big-endian order. This improves overall RocksDB throughput, as cubes/columns that are near to each other will likely be stored
 * close together on disk as well.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class PositionSerializerUtils {
    public static final int SIZE_COLUMN_POS = Long.BYTES;
    public static final int SIZE_CUBE_POS = Integer.BYTES * 3;

    private static final ThreadLocal<ByteBuf> TEMP_BUFFER_CACHE = ThreadLocal.withInitial(() -> {
        int maxCapacity = max(SIZE_COLUMN_POS, SIZE_CUBE_POS);
        return UnpooledByteBufAllocator.DEFAULT.heapBuffer(maxCapacity, maxCapacity);
    });

    public byte[] columnPosToBytes(@NonNull ChunkPos pos) {
        ByteBuf buf = TEMP_BUFFER_CACHE.get().clear();
        writeColumnPos(buf, pos);
        return Arrays.copyOfRange(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.arrayOffset() + buf.writerIndex());
    }

    public void writeColumnPos(@NonNull ByteBuf dst, @NonNull ChunkPos pos) {
        int x = pos.x;
        int z = pos.z;
        long l = 0L;
        for (int i = 0; i < 32; i++) {
            l |= (long) ((((x >>> i) & 1) << 1) | ((z >>> i) & 1)) << (i << 1);
        }
        dst.writeLong(l);
    }

    public ChunkPos readColumnPos(@NonNull ByteBuf src) {
        long l = src.readLong();
        int x = 0;
        int z = 0;
        for (int i = 0; i < 32; i++) {
            int bits = (int) (l >>> (i << 1)) & 0x3;
            x |= (bits >> 1) << i;
            z |= (bits & 1) << i;
        }
        return new ChunkPos(x, z);
    }

    public byte[] cubePosToBytes(@NonNull CubePos pos) {
        ByteBuf buf = TEMP_BUFFER_CACHE.get().clear();
        writeCubePos(buf, pos);
        return Arrays.copyOfRange(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.arrayOffset() + buf.writerIndex());
    }

    public void writeCubePos(@NonNull ByteBuf dst, @NonNull CubePos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        dst.writeInt(0).writeInt(0).writeInt(0);
        int startIndex = dst.writerIndex() - 1;
        writeInterleavedIntBits(dst, startIndex, 3, 2, x);
        writeInterleavedIntBits(dst, startIndex, 3, 1, y);
        writeInterleavedIntBits(dst, startIndex, 3, 0, z);
    }

    protected void writeInterleavedIntBits(@NonNull ByteBuf dst, int startIndex, int nValues, int bitOffset, int value) {
        for (int shift = 0; shift < 32; shift++) {
            int bit = (value >>> shift) & 1;
            int targetBitIndex = bitOffset + shift * nValues;
            int targetByteIndex = startIndex - (targetBitIndex >>> 3);
            dst.setByte(targetByteIndex, dst.getByte(targetByteIndex) | (bit << (targetBitIndex & 0x7)));
        }
    }

    public CubePos readCubePos(@NonNull ByteBuf src) {
        src.skipBytes(SIZE_CUBE_POS);
        int startIndex = src.readerIndex() - 1;

        int x = readInterleavedIntBits(src, startIndex, 3, 2);
        int y = readInterleavedIntBits(src, startIndex, 3, 1);
        int z = readInterleavedIntBits(src, startIndex, 3, 0);
        return new CubePos(x, y, z);
    }

    protected int readInterleavedIntBits(@NonNull ByteBuf src, int startIndex, int nValues, int bitOffset) {
        int value = 0;
        for (int shift = 0; shift < 32; shift++) {
            int targetBitIndex = bitOffset + shift * nValues;
            int targetByteIndex = startIndex - (targetBitIndex >>> 3);
            int bit = (src.getByte(targetByteIndex) >>> (targetBitIndex & 0x7)) & 1;
            value |= bit << shift;
        }
        return value;
    }
}
