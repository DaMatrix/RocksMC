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
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class InputUtils {
    private final Pattern UNESCAPE_PATTERN = Pattern.compile("\\\\([\\\\\\\"])");
    private final String UNESCAPE_REPLACEMENT = "$1";

    private final Pattern SPLIT_ARGS_PATTERN = Pattern.compile("(?:^| )(['\\\"]?)(.*?)\\1(?= |$)");

    /**
     * Processes escape sequences in a quoted string.
     *
     * @param quoted the quoted string
     * @return the processed string
     */
    public String unescapeQuotedString(@NonNull String quoted) {
        return UNESCAPE_PATTERN.matcher(quoted).replaceAll(UNESCAPE_REPLACEMENT); //all escape sequences other than \\ and \" are treated literally
    }

    /**
     * Splits an argument string into individual arguments, with more or less correct handling for quotes.
     *
     * @param argsIn the argument string(s)
     * @return the split arguments
     */
    public String[] splitQuotedArguments(@NonNull String... argsIn) {
        List<String> results = new ArrayList<>();

        Matcher matcher = SPLIT_ARGS_PATTERN.matcher(String.join(" ", argsIn));
        while (matcher.find()) {
            String arg = matcher.group(2);
            if (!arg.isEmpty()) {
                results.add(arg);
            }
        }

        return results.toArray(new String[0]);
    }
}
