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

package net.daporkchop.rocksmc.storage;

import lombok.NonNull;
import net.daporkchop.rocksmc.util.InputUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.rocksmc.util.TranslationKeys.*;

/**
 * @author DaPorkchop_
 */
public class World {
    protected static final Pattern WORLD_PATTERN = Pattern.compile("(?<current>@)"
                                                                   + "|\\$(?:(?<path>[^\\\\\\\". \\n]+)|\\\"(?<qpath>(?>\\\\?[^\\n])+)\\\")"
                                                                   + "|(?:(?<name>[^\\\\\\\". \\n]+)|\\\"(?<qname>(?>\\\\?[^\\n])+)\\\")");

    public static World findWorld(@NonNull ICommandSender sender, @NonNull String id) throws CommandException {
        Matcher matcher = WORLD_PATTERN.matcher(id);
        if (!matcher.matches()) { //ensure that id is valid
            throw new CommandException(ERROR_CANNOT_PARSE_WORLD, id);
        }

        Path path;
        String s;
        if ((s = matcher.group("current")) != null) { //sender's current world
            //TODO
        } else if ((s = matcher.group("name")) != null || (s = matcher.group("qname")) != null) { //get world by name
            s = InputUtils.unescapeQuotedString(s);
            //TODO
        } else if ((s = matcher.group("path")) != null || (s = matcher.group("qpath")) != null) { //absolute world path
            path = Paths.get(InputUtils.unescapeQuotedString(s));
        }

        return null; //TODO: get world instance from Path
    }

    public Save findSave(@NonNull ICommandSender sender, @NonNull String id) throws CommandException {
        return null;
    }

    public List<String> listSaveIds(String searchPrefix) {
        return Collections.emptyList();
    }
}
