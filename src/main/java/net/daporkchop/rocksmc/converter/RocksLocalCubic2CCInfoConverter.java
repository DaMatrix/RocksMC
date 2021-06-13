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

package net.daporkchop.rocksmc.converter;

import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.util.Utils;
import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class RocksLocalCubic2CCInfoConverter implements LevelInfoConverter<RocksLocalCubicColumnData, CubicChunksColumnData> {
    @NonNull
    private final Path srcDir;
    @NonNull
    private final Path dstDir;

    @Override
    public void convert() throws IOException {
        Utils.createDirectories(dstDir);
        Utils.copyEverythingExcept(srcDir, srcDir, dstDir, file ->
                        Dimensions.getDimensions().stream().anyMatch(dim ->
                                srcDir.resolve(dim.getDirectory()).resolve("rocksmc_local").equals(file)
                        ),
                f -> {
                } // TODO: counting files
        );

        File cubicChunksData = this.dstDir.resolve("data").resolve("cubicChunksData.dat").toFile();
        NBTTagCompound nbt;
        try (InputStream in = new FileInputStream(cubicChunksData)) {
            nbt = CompressedStreamTools.readCompressed(in);
        }
        nbt.getCompoundTag("data").setString("storageFormat", StorageFormatProviderBase.DEFAULT.toString());
        try (OutputStream out = new FileOutputStream(cubicChunksData)) {
            CompressedStreamTools.writeCompressed(nbt, out);
        }
    }
}
