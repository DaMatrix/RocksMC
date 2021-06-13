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

package net.daporkchop.rocksmc.storage.local;

import cubicchunks.regionlib.util.Utils;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.rocksmc.RocksMC;
import net.daporkchop.rocksmc.RocksMCConfig;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.util.IOFunction;
import net.daporkchop.rocksmc.util.NBTSerializerUtils;
import net.daporkchop.rocksmc.util.PositionSerializerUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.FlushOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;
import static net.daporkchop.rocksmc.util.PositionSerializerUtils.*;

/**
 * Implementation of the RocksMC storage format as an {@link ICubicStorage}.
 *
 * @author DaPorkchop_
 */
public class LocalStorageImpl implements IBinaryCubeStorage {
    protected static final byte[] COLUMN_NAME_COLUMNS = "columns".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] COLUMN_NAME_CUBES = "cubes".getBytes(StandardCharsets.UTF_8);

    protected static final int INITIAL_BUFFER_SIZE = 1 << 16;

    protected static final ByteBuffer EMPTY_DIRECT_BYTEBUFFER = ByteBuffer.allocateDirect(0);

    protected static final ReadOptions READ_OPTIONS = new ReadOptions();
    protected static final WriteOptions WRITE_OPTIONS = new WriteOptions();
    protected static final FlushOptions FLUSH_OPTIONS = new FlushOptions().setWaitForFlush(true).setAllowWriteStall(true);

    protected final World world;
    @Getter
    protected final Path path;

    protected final RocksDB db;

    protected final List<ColumnFamilyHandle> cfHandles;
    protected final ColumnFamilyHandle cfHandleColumns;
    protected final ColumnFamilyHandle cfHandleCubes;

    public LocalStorageImpl(World world, @NonNull Path path) throws IOException {
        this.world = world;
        this.path = path.resolve("rocksmc_local");

        try {
            Tuple<DBOptions, ColumnFamilyOptions> options = RocksMCConfig.database.rocksOptions();

            List<ColumnFamilyDescriptor> cfDescriptors = Arrays.asList(
                    new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, options.getSecond()),
                    new ColumnFamilyDescriptor(COLUMN_NAME_COLUMNS, options.getSecond()),
                    new ColumnFamilyDescriptor(COLUMN_NAME_CUBES, options.getSecond()));
            List<ColumnFamilyHandle> cfHandles = new ArrayList<>(cfDescriptors.size());

            Path currentDir = this.path.resolve("db");
            Utils.createDirectories(currentDir);

            this.db = RocksDB.open(options.getFirst(), currentDir.toString(), cfDescriptors, cfHandles);

            this.cfHandles = cfHandles;
            this.cfHandleColumns = cfHandles.get(1);
            this.cfHandleCubes = cfHandles.get(2);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }

        if (world != null) {
            RocksMC.STORAGES_BY_WORLD.put(world, this);
        }
    }

    @Override
    public boolean columnExists(ChunkPos pos) throws IOException {
        ByteBuf keyBuf = ByteBufAllocator.DEFAULT.directBuffer(SIZE_COLUMN_POS, SIZE_COLUMN_POS);
        try {
            //encode position to bytes
            PositionSerializerUtils.writeColumnPos(keyBuf, pos);

            //issue a read without actually loading the value
            return this.db.get(this.cfHandleColumns, READ_OPTIONS, keyBuf.nioBuffer(), EMPTY_DIRECT_BYTEBUFFER) != RocksDB.NOT_FOUND;
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        } finally {
            keyBuf.release();
        }
    }

    @Override
    public boolean cubeExists(CubePos pos) throws IOException {
        ByteBuf keyBuf = ByteBufAllocator.DEFAULT.directBuffer(SIZE_CUBE_POS, SIZE_CUBE_POS);
        try {
            //encode position to bytes
            PositionSerializerUtils.writeCubePos(keyBuf, pos);

            //issue a read without actually loading the value
            return this.db.get(this.cfHandleCubes, READ_OPTIONS, keyBuf.nioBuffer(), EMPTY_DIRECT_BYTEBUFFER) != RocksDB.NOT_FOUND;
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        } finally {
            keyBuf.release();
        }
    }

    @Override
    public NBTTagCompound readColumn(ChunkPos pos) throws IOException {
        try {
            //encode position to bytes
            byte[] key = PositionSerializerUtils.columnPosToBytes(pos);

            //load from db
            byte[] data = this.db.get(this.cfHandleColumns, key);
            return data != null ? NBTSerializerUtils.readNBT(Unpooled.wrappedBuffer(data)) : null;
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public NBTTagCompound readCube(CubePos pos) throws IOException {
        try {
            //encode position to bytes
            byte[] key = PositionSerializerUtils.cubePosToBytes(pos);

            //load from db
            byte[] data = this.db.get(this.cfHandleCubes, key);
            return data != null ? NBTSerializerUtils.readNBT(Unpooled.wrappedBuffer(data)) : null;
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Nonnull
    @Override
    public NBTBatch readBatch(PosBatch positions) throws IOException {
        return this.readBaseBatch(positions, data -> data != null ? NBTSerializerUtils.readNBT(Unpooled.wrappedBuffer(data)) : null, NBTBatch::new);
    }

    @Override
    public BinaryBatch readBinaryBatch(PosBatch positions) throws IOException {
        return this.readBaseBatch(positions, data -> data != null ? Unpooled.wrappedBuffer(data) : null, BinaryBatch::new);
    }

    protected <T, B> B readBaseBatch(@NonNull PosBatch positions, @NonNull IOFunction<byte[], T> mapper, @NonNull BiFunction<Map<ChunkPos, T>, Map<CubePos, T>, B> batchCombiner) throws IOException {
        try {
            //collect positions into lists
            List<ChunkPos> columns = new ArrayList<>(positions.columns);
            List<CubePos> cubes = new ArrayList<>(positions.cubes);

            //preallocate lists for column families and encoded keys
            int totalSize = columns.size() + cubes.size();
            List<ColumnFamilyHandle> cfHandles = new ArrayList<>(totalSize);
            List<byte[]> keys = new ArrayList<>(totalSize);

            //encode positions
            for (ChunkPos pos : columns) {
                cfHandles.add(this.cfHandleColumns);
                keys.add(PositionSerializerUtils.columnPosToBytes(pos));
            }
            for (CubePos pos : cubes) {
                cfHandles.add(this.cfHandleCubes);
                keys.add(PositionSerializerUtils.cubePosToBytes(pos));
            }

            //get all values at once
            List<byte[]> values = this.db.multiGetAsList(cfHandles, keys);

            //parse values
            //TODO: this part might benefit from being parallel
            Map<ChunkPos, T> columnNbt = new Object2ObjectOpenHashMap<>(columns.size());
            Map<CubePos, T> cubeNbt = new Object2ObjectOpenHashMap<>(cubes.size());
            int i = 0;
            for (ChunkPos pos : columns) {
                columnNbt.put(pos, mapper.apply(values.get(i++)));
            }
            for (CubePos pos : cubes) {
                cubeNbt.put(pos, mapper.apply(values.get(i++)));
            }

            return batchCombiner.apply(columnNbt, cubeNbt);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public void writeColumn(ChunkPos pos, NBTTagCompound nbt) throws IOException {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(INITIAL_BUFFER_SIZE);
        try {
            //encode position
            PositionSerializerUtils.writeColumnPos(buf, pos);
            ByteBuffer nioKeyBuffer = buf.nioBuffer();

            //encode nbt
            NBTSerializerUtils.writeNBT(buf, nbt);
            ByteBuffer nioValueBuffer = buf.nioBuffer(buf.readerIndex() + SIZE_COLUMN_POS, buf.readableBytes() - SIZE_COLUMN_POS);

            //write to db
            this.db.put(this.cfHandleColumns, WRITE_OPTIONS, nioKeyBuffer, nioValueBuffer);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        } finally {
            buf.release();
        }
    }

    @Override
    public void writeCube(CubePos pos, NBTTagCompound nbt) throws IOException {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(INITIAL_BUFFER_SIZE);
        try {
            //encode position
            PositionSerializerUtils.writeCubePos(buf, pos);
            ByteBuffer nioKeyBuffer = buf.nioBuffer();

            //encode nbt
            NBTSerializerUtils.writeNBT(buf, nbt);
            ByteBuffer nioValueBuffer = buf.nioBuffer(buf.readerIndex() + SIZE_CUBE_POS, buf.readableBytes() - SIZE_CUBE_POS);

            //write to db
            this.db.put(this.cfHandleCubes, WRITE_OPTIONS, nioKeyBuffer, nioValueBuffer);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        } finally {
            buf.release();
        }
    }

    @Override
    public void writeBatch(NBTBatch batch) throws IOException {
        this.writeBaseBatch(batch.columns, batch.cubes, NBTSerializerUtils::writeNBT);
    }

    @Override
    public void writeBinaryBatch(BinaryBatch batch) throws IOException {
        this.writeBaseBatch(batch.columns, batch.cubes, ByteBuf::writeBytes);
    }

    protected <T> void writeBaseBatch(@NonNull Map<ChunkPos, T> columns, @NonNull Map<CubePos, T> cubes, @NonNull BiConsumer<ByteBuf, T> encoder) throws IOException {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(INITIAL_BUFFER_SIZE);
        try (WriteBatch dst = new WriteBatch()) {
            columns.forEach(new BiConsumer<ChunkPos, T>() {
                @Override
                @SneakyThrows(RocksDBException.class)
                public void accept(ChunkPos pos, T nbt) {
                    buf.clear();

                    //encode position
                    PositionSerializerUtils.writeColumnPos(buf, pos);
                    ByteBuffer nioKeyBuffer = buf.nioBuffer();

                    //encode data
                    encoder.accept(buf, nbt);
                    ByteBuffer nioValueBuffer = buf.nioBuffer(buf.readerIndex() + SIZE_COLUMN_POS, buf.readableBytes() - SIZE_COLUMN_POS);

                    dst.put(LocalStorageImpl.this.cfHandleColumns, nioKeyBuffer, nioValueBuffer);
                }
            });
            cubes.forEach(new BiConsumer<CubePos, T>() {
                @Override
                @SneakyThrows(RocksDBException.class)
                public void accept(CubePos pos, T nbt) {
                    buf.clear();

                    //encode position
                    PositionSerializerUtils.writeCubePos(buf, pos);
                    ByteBuffer nioKeyBuffer = buf.nioBuffer();

                    //encode data
                    encoder.accept(buf, nbt);
                    ByteBuffer nioValueBuffer = buf.nioBuffer(buf.readerIndex() + SIZE_CUBE_POS, buf.readableBytes() - SIZE_CUBE_POS);

                    dst.put(LocalStorageImpl.this.cfHandleCubes, nioKeyBuffer, nioValueBuffer);
                }
            });

            //write to db
            this.db.write(WRITE_OPTIONS, dst);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        } finally {
            buf.release();
        }
    }

    @Override
    public void forEachColumn(Consumer<ChunkPos> callback) throws IOException {
        ByteBuf keyBuf = ByteBufAllocator.DEFAULT.directBuffer(SIZE_COLUMN_POS, SIZE_COLUMN_POS).writerIndex(SIZE_COLUMN_POS);
        ByteBuffer nioKeyBuffer = keyBuf.nioBuffer();
        try (RocksIterator itr = this.db.newIterator(this.cfHandleColumns)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                //copy key into NIO buffer
                nioKeyBuffer.clear();
                checkState(itr.key(nioKeyBuffer) == SIZE_COLUMN_POS, "column position too large!");

                //parse key
                ChunkPos pos = PositionSerializerUtils.readColumnPos(keyBuf.readerIndex(0));
                callback.accept(pos);
            }
        } finally {
            keyBuf.release();
        }
    }

    @Override
    public void forEachCube(Consumer<CubePos> callback) throws IOException {
        ByteBuf keyBuf = ByteBufAllocator.DEFAULT.directBuffer(SIZE_CUBE_POS, SIZE_CUBE_POS).writerIndex(SIZE_CUBE_POS);
        ByteBuffer nioKeyBuffer = keyBuf.nioBuffer();
        try (RocksIterator itr = this.db.newIterator(this.cfHandleCubes)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                //copy key into NIO buffer
                nioKeyBuffer.clear();
                checkState(itr.key(nioKeyBuffer) == SIZE_CUBE_POS, "cube position too large!");

                //parse key
                CubePos pos = PositionSerializerUtils.readCubePos(keyBuf.readerIndex(0));
                callback.accept(pos);
            }
        } finally {
            keyBuf.release();
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            this.db.flush(FLUSH_OPTIONS, this.cfHandles);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public void close() throws IOException {
        try { //attempt to flush the WAL immediately, in order to prevent (uncompressed) log files from sitting around forever
            this.flush();
        } finally {
            checkState(this.world == null || RocksMC.STORAGES_BY_WORLD.remove(this.world, this), "unable to remove self from storages map!");

            this.cfHandles.forEach(ColumnFamilyHandle::close); //close column families before db
            this.db.close();
        }
    }
}
