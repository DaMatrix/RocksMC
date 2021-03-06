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

import cubicchunks.converter.gui.GuiFrame;
import cubicchunks.converter.gui.GuiMain;
import cubicchunks.converter.lib.Registry;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import lombok.SneakyThrows;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicColumnData;
import net.daporkchop.rocksmc.converter.dataconverter.CC2RocksLocalCubicDataConverter;
import net.daporkchop.rocksmc.converter.dataconverter.RocksLocalCubic2CCDataConverter;
import net.daporkchop.rocksmc.converter.infoconverter.CC2RocksLocalCubicInfoConverter;
import net.daporkchop.rocksmc.converter.infoconverter.RocksLocalCubic2CCInfoConverter;
import net.daporkchop.rocksmc.converter.io.RocksLocalCubicReader;
import net.daporkchop.rocksmc.converter.io.RocksLocalCubicWriter;

import javax.swing.WindowConstants;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;

/**
 * Delegating main class for the converter which injects the RocksMC formats into the CubicChunksConverter registry before loading.
 *
 * @author DaPorkchop_
 */
public class ConverterMain {
    static {
        if (!GraphicsEnvironment.isHeadless()) {
            GuiFrame.DEFAULT_CLOSE_OPERATION = WindowConstants.DISPOSE_ON_CLOSE;
        }

        Registry.registerReader("RocksMC (CubicChunks, Local)", RocksLocalCubicReader::new, RocksLocalCubicColumnData.class);

        Registry.registerWriter("RocksMC (CubicChunks, Local)", RocksLocalCubicWriter::new, RocksLocalCubicColumnData.class);

        Registry.registerConverter("Default", RocksLocalCubic2CCDataConverter::new, RocksLocalCubic2CCInfoConverter::new, RocksLocalCubicColumnData.class, CubicChunksColumnData.class, RocksLocalCubic2CCDataConverter.class);
        Registry.registerConverter("Default", CC2RocksLocalCubicDataConverter::new, CC2RocksLocalCubicInfoConverter::new, CubicChunksColumnData.class, RocksLocalCubicColumnData.class, CC2RocksLocalCubicDataConverter.class);
    }

    @SneakyThrows({ InterruptedException.class, InvocationTargetException.class })
    public static void main(String... args) {
        GuiMain.main(args);
    }
}
