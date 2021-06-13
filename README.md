# RocksMC

[![Build Status](https://jenkins.daporkchop.net/job/DaPorkchop_/job/RocksMC/job/master/badge/icon)](https://jenkins.daporkchop.net/job/DaPorkchop_/job/RocksMC/)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/DaMatrix/RocksMC)
![Lines of code](https://img.shields.io/tokei/lines/github/DaMatrix/RocksMC)
[![Discord](https://img.shields.io/discord/428813657816956929?color=7289DA&label=discord)](https://discord.gg/FrBHHCk)
[![Patreon badge](https://img.shields.io/badge/dynamic/json?color=e64413&label=patreon&query=data.attributes.patron_count&suffix=%20patrons&url=https%3A%2F%2Fwww.patreon.com%2Fapi%2Fcampaigns%2F727078)](https://www.patreon.com/DaPorkchop_)

*A fast, compact and powerful storage format for Minecraft, built using [rocksdb](https://github.com/facebook/rocksdb).*  
*Currently only supports [Cubic Chunks](https://github.com/OpenCubicChunks/CubicChunks).*

## TODO:

- global saves
- snapshots
- conversion to/from standard world formats

## Concepts

***NOTE: Currently, only local saves are implemented, global saves and snapshots are not.***

### World

A Minecraft world. Consists of one or more dimensions.  
On the client, worlds are stored in individual subdirectories of `.minecraft/saves/` (
e.g. `.minecraft/saves/New World/`). Servers only have a single world, which is stored in a single directory (
named `world` by default).

### Dimension

A dimension in a Minecraft world. Generally identified by name (e.g. `minecraft:overworld`, `minecraft:nether`), or by
ID (e.g. `0`, `1`, `-1`). Dimensions are each stored in separate folders. The default world (dimension 0,
or `minecraft:overworld`) is stored in `<world_directory>/`, all other dimensions are stored
in `<world_directory>/DIM<dimension_id>/`.  
When using [Cubic Chunks](https://github.com/OpenCubicChunks/CubicChunks), each dimension can use a different storage
format.

### Save

A save consists of a directory with two children:

- `db/`: contains the RocksDB
- `snapshots/`: contains some number of subdirectories, each of which represents a RocksMC snapshot and contains a
  RocksDB database checkpoint

#### Local Save (Storage Format ID: `rocksmc:local`)

A local save belongs to a single dimension using the `rocksmc:local` storage format, and is stored
in `<dimension_directory>/rocksmc_local/`.

#### Global Save (Storage Format ID: `rocksmc:global`)

A global save belongs to a world, and is stored in `<world directory>/rocksmc_global/`. Its RocksDB contains one storage
for every dimension in the world that uses the `rocksmc:global` storage format. They are kept separate by storing them
in individual [column families](https://github.com/facebook/rocksdb/wiki/Column-Families).

### Storage

A storage contains the terrain data for a single dimension - the exact same data as would have been stored in the region
files in a vanilla world.  
Conceptually, storages are implemented as a wrapper over some number of RocksDB column families.

## Local vs Global

The primary advantage of global saves is that all dimensions can be stored in a single RocksDB, which provides three
primary benefits:

- crash consistency across the entire world - in the event of a crash, all dimensions are guaranteed to be at the same
  revision
- global, atomic snapshots - snapshots will contain all dimensions at once at the same instance in time
- open files limit - if multiple dimensions are open as local storages, they will each track their open files limit
  individually. On systems where the maximum number of open files is restricted, this can become a serious obstacle for
  large worlds

However, for the vast majority of users, these benefits will be of little to no significance, especially compared to the
simplicity of being able to manage dimensions by manually working with the files and directories on disk (e.g. resetting
the terrain in a dimension by simply deleting the directory).

## Identifying RocksMC objects

Virtually every RocksMC command requires a save, storage or snapshot as an argument. This section explains the format
for identifying these objects.

If any part of an identifier contains whitespace or a `.`, that part must be placed in double quotes. For example, `Hello World!`
should be written as `"Hello World!"`.

### Worlds

All RocksMC objects belong to a world, which are identified by their directory name. Some examples:

<table>
<thead>
<tr>
<th>Side</th>
<th>Path to world directory</th>
<th>RocksMC world identifier</th>
<th>Comment</th>
</tr>
</thead>
<tbody>
<tr>
<td>Client</td>
<td><code>.minecraft/saves/test/</code></td>
<td><code>test</code></td>
<td></td>
</tr>
<tr>
<td>Client</td>
<td><code>.minecraft/saves/New World/</code></td>
<td><code>"New World"</code></td>
<td>Double quotes used because the name contains a space</td>
</tr>
<tr>
<td>Server</td>
<td><code>world/</code></td>
<td><code>world</code></td>
<td></td>
</tr>
<tr>
<td>Client/Server</td>
<td><em>currently open world</em></td>
<td><code>@</code></td>
<td></td>
</tr>
<tr>
<td>Client/Server</td>
<td><code>/home/user/Downloads/cool_world</code></td>
<td><code>$/home/user/Downloads/cool_world</code></td>
<td><code>$</code> symbol indicates an absolute file path</td>
</tr>
<tr>
<td>Client/Server</td>
<td><code>C:\Users\user\Downloads\Cool World</code></td>
<td><code>$"C:\Users\user\Downloads\Cool World"</code></td>
<td><code>$</code> comes <em>before</em> quotes</td>
</tr>
</tbody>
</table>

### Saves

Assuming you have a world, you can now access the individual saves inside it.

<table>
<thead>
<tr>
<th>Save Type</th>
<th>Dimension</th>
<th>RocksMC save identifier</th>
<th>Comment</th>
</tr>
</thead>
<tbody>
<tr>
<td>Local</td>
<td><code>-1</code></td>
<td><code>DIM-1</code></td>
<td></td>
</tr>
<tr>
<td>Local</td>
<td><em>default dimension</em></td>
<td><code>d</code></td>
<td><code>d</code> stands for “default”. This is generally dimension 0 (the overworld)</td>
</tr>
<tr>
<td>Global</td>
<td></td>
<td><code>*</code></td>
<td>Global save doesn’t need a dimension</td>
</tr>
<tr>
<td>Local/Global</td>
<td><em>current dimension</em></td>
<td><code>@</code></td>
<td>Finds the save used by the dimension that the command sender is currently in. Cannot be used from the server console</td>
</tr>
</tbody>
</table>

### Snapshots

Identifying individual snapshots inside a save is quite trivial: simply enter the name.

<table>
<thead>
<tr>
<th>Snapshot name</th>
<th>RocksMC snapshot identifier</th>
<th>Comment</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>asdf</code></td>
<td><code>asdf</code></td>
<td></td>
</tr>
<tr>
<td><code>my new snapshot</code></td>
<td><code>"my new snapshot"</code></td>
<td>Whitespace still needs to be quoted</td>
</tr>
<tr>
<td><em>latest snapshot</em></td>
<td><code>^</code></td>
<td>Computed based on filesystem creation time for the snapshot directory</td>
</tr>
</tbody>
</table>

### Putting it all together

These three parts are concatenated together (using a `.`) in order to build full object references. You can't access a snapshot without a save, or a save without a world. Thus, the full identifier syntax is:

```
<world_identifier>[.<save_identifier>[.<snapshot_identifier>]]
```
