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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

/**
 * Utilities for reading/writing NBT data to/from Netty {@link ByteBuf}s.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class NBTSerializerUtils {
    public ByteBuffer toHeapNioBuffer(ByteBuf src) {
        if (src == null) {
            return null;
        }

        try {
            if (src.hasArray() && src.alloc() instanceof UnpooledByteBufAllocator) {
                return ByteBuffer.wrap(src.array(), src.arrayOffset() + src.readerIndex(), src.readableBytes());
            } else {
                ByteBuffer dst = ByteBuffer.allocate(src.readableBytes());
                src.readBytes(dst);
                dst.flip();
                return dst;
            }
        } finally {
            ReferenceCountUtil.release(src);
        }
    }

    @SneakyThrows(IOException.class)
    public ByteBuffer compressForCubicChunks(ByteBuffer src) {
        if (src == null) {
            return null;
        }

        ByteBuf tmp = UnpooledByteBufAllocator.DEFAULT.buffer(src.remaining());
        try (OutputStream out = new GZIPOutputStream(new ByteBufOutputStream(tmp))) {
            out.write(src.array(), src.arrayOffset(), src.remaining());
        }
        return toHeapNioBuffer(tmp);
    }

    @SneakyThrows(IOException.class)
    public void writeNBT(@NonNull ByteBuf dst, @NonNull NBTTagCompound nbt) {
        //this doesn't actually do compression
        CompressedStreamTools.write(nbt, new ByteBufOutputStream(dst));
    }

    @SneakyThrows(IOException.class)
    public NBTTagCompound readNBT(@NonNull ByteBuf src) {
        //this doesn't actually do decompression
        return CompressedStreamTools.read(new ByteBufInputStream(src), NBTSizeTracker.INFINITE);
    }
}
