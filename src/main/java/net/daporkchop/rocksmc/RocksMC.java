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

package net.daporkchop.rocksmc;

import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import net.daporkchop.rocksmc.command.CommandRocks;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;
import net.daporkchop.rocksmc.storage.local.LocalStorageProvider;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDB;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
@Mod(modid = RocksMC.MODID,
        dependencies = "required-after:cubicchunks@[1.12.2-0.0.1189.0,)",
        acceptableRemoteVersions = "*",
        useMetadata = true)
public class RocksMC {
    public static final String MODID = "rocksmc";

    public static final Map<World, LocalStorageImpl> STORAGES_BY_WORLD = Collections.synchronizedMap(new IdentityHashMap<>());
    public static Logger LOGGER;

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        RocksDB.loadLibrary(); //ensure native lib is loaded
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerStorageFormats(RegistryEvent.Register<StorageFormatProviderBase> event) {
        event.getRegistry().register(new LocalStorageProvider());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandRocks());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        //close all storages that might have been left open
        STORAGES_BY_WORLD.values().removeIf(storage -> {
            LOGGER.warn("RocksMC storage in \"{}\" wasn't closed!", storage.path());
            try {
                storage.close();
            } catch (IOException e) {
                LOGGER.error("Exception while closing storage in " + storage.path(), e);
            }
            return true; //remove everything lol
        });
    }
}
