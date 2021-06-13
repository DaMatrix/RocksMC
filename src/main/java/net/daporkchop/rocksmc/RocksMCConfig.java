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

import net.minecraft.util.Tuple;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.Env;

/**
 * RocksCC configuration.
 *
 * @author DaPorkchop_
 */
@Config(modid = RocksMC.MODID)
@Config.RequiresWorldRestart
@Mod.EventBusSubscriber(modid = RocksMC.MODID)
public class RocksMCConfig {
    @Config.Comment("Whether or not to allow RocksMC to be used as the default world format when no others are available.")
    public static boolean allowDefault = true;

    @Config.Comment({
            "Configuration options used for opening the database.",
            "Generally speaking, the default options should be fine for virtually every user, and changing them can have serious implications for performance",
            "and/or disk usage. Do not touch unless you know what you're doing!"
    })
    @Config.RequiresWorldRestart
    public static DB database = new DB();

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (RocksMC.MODID.equals(event.getModID())) {
            notifyChanged();
        }
    }

    /**
     * Notifies the configuration that it has changed.
     */
    public static void notifyChanged() {
        ConfigManager.sync(RocksMC.MODID, Config.Type.INSTANCE);

        database.notifyChanged();
    }

    /**
     * Options used for opening the database.
     *
     * @author DaPorkchop_
     */
    public static class DB {
        @Config.Comment({
                "The number of threads to use for flush and compactions.",
                "Default: CPU count"
        })
        @Config.RangeInt(min = 1)
        public int parallelism = Runtime.getRuntime().availableProcessors();

        @Config.Comment({
                "Whether or not to do paranoid validation of checksums.",
                "Default: false"
        })
        public boolean paranoidChecks = false;

        @Config.Comment({
                "The number of threads to use when opening SST files.",
                "Only has an effect if maxOpenFiles=-1.",
                "Default: 16"
        })
        @Config.RangeInt(min = 1)
        public int fileOpeningThreads = 16;

        @Config.Comment({
                "If true, writing to SSTs will issue an fsync instead of an fdatasync.",
                "Should only be used if using a filesystem such as ext3 which can lose files during a crash.",
                "Default: false"
        })
        public boolean useFsync = false;

        @Config.Comment({
                "The maximum number of threads that will concurrently perform a compaction job by breaking it into multiple, smaller ones that are run simultaneously.",
                "Default: 1 (no subcompactions)"
        })
        @Config.RangeInt(min = 1)
        public int maxSubcompactions = 1;

        @Config.Comment({
                "Whether or not to use direct I/O for reads, bypassing the operating system's disk cache.",
                "Default: false"
        })
        public boolean directReads = false;

        @Config.Comment({
                "Whether or not to use direct I/O for writes, bypassing the operating system's disk cache.",
                "Default: false"
        })
        public boolean directWrites = false;

        @Config.Comment({
                "Whether or not to use memory-mapped I/O for reads.",
                "Default: false"
        })
        public boolean mmapReads = false;

        @Config.Comment({
                "Whether or not to use memory-mapped I/O for writes.",
                "Default: false"
        })
        public boolean mmapWrites = false;

        @Config.Comment({
                "Whether or not to hint to the operating system that access to SST files will be random.",
                "Default: true"
        })
        public boolean adviseRandom = true;

        @Config.Comment({
                "The number of bytes to store in the write buffer before flushing to disk (in KiB).",
                "If 0, write buffering will be disabled.",
                "Default: 0"
        })
        @Config.RangeInt(min = 0)
        public int writeBufferSize = 0;

        @Config.Comment({
                "Whether or not to allow multiple threads to write to the memtable at once.",
                "Default: true"
        })
        public boolean allowConcurrentMemtableWrite = true;

        @Config.Comment({
                "If true, we won't update the statistics used to optimize compaction decisions by loading table properties from many SST files.",
                "Turning this on will improve initial world load times for huge worlds, especially when running on slow storage.",
                "Default: false"
        })
        public boolean skipStatsUpdateOnDbOpen = false;

        @Config.Comment({
                "If false, the WAL will be automatically flushed to disk after every write.",
                "Default: false"
        })
        public boolean manualWalFlush = false;

        @Config.Comment({
                "The maximum number of concurrent background jobs (both flushes and compactions combined).",
                "If set to 0, the value of 'parallelism' will be used.",
                "Default: 2"
        })
        @Config.RangeInt(min = 0)
        public int maxBackgroundJobs = 0;

        @Config.Comment({
                "The maximum number of files that may be open at once.",
                "A value of -1 is equivalent to no limit.",
                "Default: -1"
        })
        public int maxOpenFiles = -1;

        @Config.Comment({
                "The compression algorithm to be used for compressing SST files.",
                "Note that changing this will only affect newly created SSTs. Data already stored in the database will remain unchanged until",
                "overwritten or included in a subsequent database compaction.",
                "Warning: do not use DISABLE_COMPRESSION_OPTION! If you want to disable compression (e.g. because you're using filesystem-level compression), use NO_COMPRESSION.",
                "Default: ZSTD_COMPRESSION"
        })
        public CompressionType compression = CompressionType.ZSTD_COMPRESSION;

        @Config.Comment({
                "The target size for level-1 SST files (in KiB).",
                "Default: 64MiB (65536KiB)"
        })
        @Config.RangeInt(min = 1)
        public int tableSizeBase = 65536;

        @Config.Comment({
                "The target file size multiplier for SST files at levels 2 and above.",
                "For example, using tableSizeBase=1MiB and tableSizeMultiplier=2, level-1 SSTs will be ~1MiB, level-2 SSTs ~2MiB, level-3 SSTs ~4MiB, and so on.",
                "Default: 1 (SSTs are the same size at all levels)"
        })
        @Config.RangeInt(min = 1)
        public int tableSizeMultiplier = 1;

        @Config.Comment({
                "The maximum number of write buffers to mantain at once.",
                "Having this set to a high number allows further writes to occur while filled write buffers are being compacted in the background.",
                "If set to 0, the value of 'parallelism' will be used.",
                "Default: 0"
        })
        @Config.RangeInt(min = 0)
        public int maxWriteBufferNumber = 0;

        @Config.Comment({
                "The maximum number of write buffers to merge into a single L0 table during compaction.",
                "If set to 0, the value of 'parallelism' will be used.",
                "Default: 0"
        })
        @Config.RangeInt(min = 0)
        public int minWriteBufferNumberToMerge = 0;

        @Config.Ignore
        protected transient volatile Tuple<DBOptions, ColumnFamilyOptions> options;

        /**
         * @return the currently configured RocksDB database open options
         */
        public synchronized Tuple<DBOptions, ColumnFamilyOptions> rocksOptions() {
            Tuple<DBOptions, ColumnFamilyOptions> options = this.options;
            if (options == null) {
                //TODO: there are a LOT more options
                this.options = options = new Tuple<>(
                        new DBOptions()
                                .setCreateIfMissing(true)
                                .setCreateMissingColumnFamilies(true)
                                .setEnv(Env.getDefault().setBackgroundThreads(this.parallelism))
                                .setIncreaseParallelism(this.parallelism)
                                .setParanoidChecks(this.paranoidChecks)
                                .setMaxFileOpeningThreads(this.fileOpeningThreads)
                                .setUseFsync(this.useFsync)
                                .setMaxSubcompactions(this.maxSubcompactions)
                                .setUseDirectReads(this.directReads)
                                .setUseDirectIoForFlushAndCompaction(this.directWrites)
                                .setAllowMmapReads(this.mmapReads)
                                .setAllowMmapWrites(this.mmapWrites)
                                .setAdviseRandomOnOpen(this.adviseRandom)
                                .setDbWriteBufferSize((long) this.writeBufferSize << 10L)
                                .setAllowConcurrentMemtableWrite(this.allowConcurrentMemtableWrite)
                                .setSkipStatsUpdateOnDbOpen(this.skipStatsUpdateOnDbOpen)
                                .setManualWalFlush(this.manualWalFlush)
                                .setMaxBackgroundJobs(this.maxBackgroundJobs == 0 ? this.parallelism : this.maxBackgroundJobs)
                                .setMaxOpenFiles(this.maxOpenFiles),
                        new ColumnFamilyOptions()
                                .setMaxWriteBufferNumber(this.maxWriteBufferNumber == 0 ? this.parallelism : this.maxWriteBufferNumber)
                                .setMinWriteBufferNumberToMerge(this.minWriteBufferNumberToMerge == 0 ? this.parallelism : this.minWriteBufferNumberToMerge)
                                .setCompressionType(this.compression)
                                .setTargetFileSizeBase((long) this.tableSizeBase << 10L)
                                .setTargetFileSizeMultiplier(this.tableSizeMultiplier));
            }
            return options;
        }

        /**
         * Notifies the configuration that it has changed.
         */
        public void notifyChanged() {
            this.options = null;
        }
    }
}
