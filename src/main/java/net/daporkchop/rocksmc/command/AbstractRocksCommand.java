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

package net.daporkchop.rocksmc.command;

import lombok.NonNull;
import net.daporkchop.rocksmc.RocksMC;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.rocksmc.util.TranslationKeys.*;

/**
 * Base class for all {@code /rockscc} subcommands.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractRocksCommand extends CommandBase {
    private final Map<String, CommandBase> children = new HashMap<>();

    public AbstractRocksCommand(@NonNull CommandBase... children) {
        for (CommandBase child : children) {
            Stream.concat(Stream.of(child.getName()), child.getAliases().stream()).distinct().forEach(name -> this.children.put(name, child));
        }
    }

    protected LocalStorageImpl getStorage(@NonNull MinecraftServer server, @NonNull String name) throws CommandException {
        int id; //attempt to parse dimension id
        try {
            id = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            throw new CommandException(ERROR_CANNOT_PARSE_DIM, name);
        }

        WorldServer world = DimensionManager.getWorld(id); //find loaded dimension with id
        if (world == null) {
            throw new CommandException(ERROR_CANNOT_FIND_DIM, id);
        }

        LocalStorageImpl storage = RocksMC.STORAGES_BY_WORLD.get(world); //find rockscc storage for world
        if (storage == null) {
            throw new CommandException(ERROR_NOT_ROCKSMC, id);
        }

        return storage;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!this.children.isEmpty() && args.length >= 1) {
            Optional<Map.Entry<String, CommandBase>> exactMatch = this.children.entrySet().stream().filter(e -> args[0].equals(e.getKey())).findAny();
            if (exactMatch.isPresent()) {
                exactMatch.get().getValue().execute(server, sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            Optional<Map.Entry<String, CommandBase>> exactMatch = this.children.entrySet().stream().filter(e -> args[0].equals(e.getKey())).findAny();
            if (exactMatch.isPresent()) {
                return exactMatch.get().getValue().getTabCompletions(server, sender, Arrays.copyOfRange(args, 1, args.length), targetPos);
            }
        }
        if (args.length <= 1) {
            return this.children.keySet().stream().filter(s -> args.length == 0 || s.startsWith(args[0])).collect(Collectors.toList());
        }

        return super.getTabCompletions(server, sender, args, targetPos);
    }
}
