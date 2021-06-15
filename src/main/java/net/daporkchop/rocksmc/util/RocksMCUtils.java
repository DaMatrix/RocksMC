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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class RocksMCUtils {
    protected final SizeFormat[] FORMATS = {
            new SizeFormat(1L << 60L, "%.2fEiB"),
            new SizeFormat(1L << 50L, "%.2fPiB"),
            new SizeFormat(1L << 40L, "%.2fTiB"),
            new SizeFormat(1L << 30L, "%.2fGiB"),
            new SizeFormat(1L << 20L, "%.2fMiB"),
            new SizeFormat(1L << 10L, "%.2fKiB")
    };

    public String formatSize(long bytes) {
        for (SizeFormat format : FORMATS) {
            if (abs(bytes) >= format.threshold) {
                return String.format(format.format, bytes / (double) format.threshold);
            }
        }
        return bytes + "B";
    }

    @RequiredArgsConstructor
    protected static final class SizeFormat {
        protected final long threshold;
        @NonNull
        protected final String format;
    }
}
