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

import lombok.experimental.UtilityClass;

import static net.daporkchop.rocksmc.RocksMC.*;

/**
 * Translation keys used throughout the mod.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class TranslationKeys {
    public final String ERROR_CANNOT_PARSE_DIM = MODID + ".error.cannot_parse_dim";
    public final String ERROR_CANNOT_FIND_DIM = MODID + ".error.cannot_find_dim";
    public final String ERROR_NOT_ROCKSMC = MODID + ".error.not_rocksmc";

    public final String ERROR_CANNOT_PARSE_WORLD = MODID + ".error.cannot_parse_world";
}
