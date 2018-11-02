# DXVK cache pool

Share your DXVK pipeline cache states for smoother (wine) gaming!

This only works for regular wine prefixes, as steam/proton does it's own thing, and probably much better.

Client:
- Fetches missing DxvkStateCacheEntry's and patches the .dxvk-cache.
- Submits DxvkStateCacheEntry's generated locally since the last run.

Not affiliated with the DXVK project, please don't blame him if this destroys your cache files.

## Usage

The client requires Java >= 8.

### Client

```bash
$ ./dxvk-cache-client -h
usage: dvxk-cache-client  directory... [--download-verified] [-h] [--host
       <url>] [--init-keys] [--min-signatures <count>] [--non-recursive]
       [--only-verified] [--verbose]
    --download-verified        Download verified public keys and
                               associated verification data
 -h,--help                     show this help
    --host <url>               Server URL
    --init-keys                Ensure keys exist and exit
    --min-signatures <count>   Minimum required signatures to download a
                               cache entry
    --non-recursive            Do not scan direcories recursively
    --only-verified            Only download entries from verified
                               uploaders
    --verbose                  Verbose output
```

### Environment

For wine to use the shared caches you should set the DXVK_STATE_CACHE_PATH environment variable and point it to either:
- `$XDG_CACHE_HOME/dxvk-cache-pool` will work for most people
- or `c:/dxvk-cache-pool` if you did sandbox your wine prefix (`winetricks sandbox`) as in that case wine can't access your home directory. You need to run `dxvk-cache-client` against all your wine prefixes in this case.

This environment variable can be setup globally by putting [dxvk-cache-pool.sh](dxvk-cache-pool.sh) into `/etc/profile.d/`. The arch package already includes it.

It doesn't affect steam/proton as proton is doing its own thing and overrides that variable.

### Example

Assuming the game files are stored somewhere in `/usr/local/games/wine`, you can run it like so:

```bash
$ ./dxvk-cache-client /usr/local/games/wine
scanning directories
scanned 77522 files
preparing wine prefixes
looking up state caches for 235 possible games
found 3 matching caches
writing 1 new caches
 -> writing witcher3 to /home/poison/.cache/dxvk/witcher3.dxvk-cache
updating 2 caches
 -> ManiaPlanet is up to date with 473 entries
 -> ManiaPlanet sending 31 missing entries to remote
 -> patching Beat Saber with 521 entries, adding 32 entries
found 0 candidates for upload
```

You can pass multiple game directories. For each passed directory, the client will recursively search for exe files and automatically create the .dxvk-cache's for you if they did not already exist.

Alternatively, if the .dxvk-cache's already exist (i.e. you have already ran each game at least once), you can pass no directories at all like so:

```bash
$ dxvk-cache-client 
target directory is: /home/owner/.cache/dxvk-cache-pool
scanning directories
 -> scanned 3 files
preparing wine prefixes
looking up remote caches for 2 possible games
 -> found 2 matching caches
writing 0 new caches
updating 2 caches
 -> QuakeChampions: is up to date (2008 entries)
 -> UE4-Win64-Shipping: is up to date (2928 entries)
found 0 candidates for upload
```

If you are using a sandboxed wine prefix, then the directories should be wine prefixes instead of game files (with game files and binaries stored in the prefix).

## Security

All state cache entries are signed.


### Submission

The client will automatically generate a key pair on the first run.
Every entry uploaded by the client will be signed with your public key.
The signature for each entry is validated on the server and kept for every uploader.


### Download

The user decides on the desired level of security.
By default only cache entries with more than two signature will be downloaded.
You can opt to only download cache entries which are signed by verified users (`--only-verified`).

### Becoming a verified user

See [Verification](Verification.md).

## FAQ

For frequently asked questions, see [FAQ](FAQ.md)

## Building

See [Building](Building.md)

## Server

For server documentation, see the [server README](dxvk-cache-pool-server/README.md).
