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

package net.daporkchop.rocksmc.converter.infoconverter;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.util.Utils;
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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractRocksCCInfoConverter<IN, OUT> implements LevelInfoConverter<IN, OUT> {
    @NonNull
    private final Path srcDir;
    @NonNull
    private final Path dstDir;

    @Override
    public void convert() throws IOException {
        Utils.createDirectories(this.dstDir);
        Utils.copyEverythingExcept(this.srcDir, this.srcDir, this.dstDir, new HashSet<>(this.ignorePaths(this.srcDir))::contains, f -> { });

        File cubicChunksData = this.dstDir.resolve("data").resolve("cubicChunksData.dat").toFile();
        NBTTagCompound nbt;
        try (InputStream in = new FileInputStream(cubicChunksData)) {
            nbt = CompressedStreamTools.readCompressed(in);
        }
        nbt.getCompoundTag("data").setString("storageFormat", this.newFormatName());
        try (OutputStream out = new FileOutputStream(cubicChunksData)) {
            CompressedStreamTools.writeCompressed(nbt, out);
        }
    }

    protected List<Path> ignorePaths(@NonNull Path srcDir) {
        return Dimensions.getDimensions().stream().flatMap(dim -> this.ignorePaths(srcDir, dim).stream()).collect(Collectors.toList());
    }

    protected abstract List<Path> ignorePaths(@NonNull Path srcDir, @NonNull Dimension dim);

    protected abstract String newFormatName();
}
