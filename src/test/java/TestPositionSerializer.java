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

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import net.daporkchop.rocksmc.util.PositionSerializerUtils;
import net.minecraft.util.math.ChunkPos;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.*;

/**
 * @author DaPorkchop_
 */
public class TestPositionSerializer {
    @Test
    public void testColumns() {
        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer();
        try {
            ChunkPos[] positions = IntStream.range(0, 10000)
                    .mapToObj(i -> new ChunkPos(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt()))
                    .peek(pos -> PositionSerializerUtils.writeColumnPos(buf, pos))
                    .toArray(ChunkPos[]::new);

            for (ChunkPos orig : positions) {
                ChunkPos read = PositionSerializerUtils.readColumnPos(buf);
                checkState(orig.equals(read), "original position %s is different from deserialized position %s!", orig, read);
            }
        } finally {
            buf.release();
        }
    }

    @Test
    public void testCubes() {
        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer();
        try {
            CubePos[] positions = IntStream.range(0, 10000)
                    .mapToObj(i -> new CubePos(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt()))
                    .peek(pos -> PositionSerializerUtils.writeCubePos(buf, pos))
                    .toArray(CubePos[]::new);

            for (CubePos orig : positions) {
                CubePos read = PositionSerializerUtils.readCubePos(buf);
                checkState(orig.equals(read), "original position %s is different from deserialized position %s!", orig, read);
            }
        } finally {
            buf.release();
        }
    }
}
