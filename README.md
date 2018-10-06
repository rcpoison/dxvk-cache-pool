# DXVK cache pool (Beta)

Client/server to share DXVK pipeline caches.

This only works for regular wine prefixes, as steam/proton does it's own thing, and probably much better.

Client:
- Fetches missing DxvkStateCacheEntry's and patches the .dxvk-cache.
- Submits DxvkStateCacheEntry's generated locally since the last run.

Server:
- Centralized storage. Provides REST interface to access caches.


Not affiliated with the DXVK project, please don't blame him if this destroys your cache files.

## Building

Prerequisites:
- maven 3
- openjdk >= 8

Build: 
```bash
./build.sh
```

Executables:
```bash
dxvk-cache-client
dxvk-cache-server
```

Archlinux:

See [PKGBUILD](arch/PKGBUILD)


## Usage

Both client and server require Java >= 8.

### Client

```bash
$ ./dxvk-cache-client -h
usage: dvxk-cache-client  directory... [--download-verified] [-h] [--host
       <url>] [--init-keys] [--non-recursive] [--only-verified]
       [--verbose]
    --download-verified   Download verified public keys and associated
                          verification data
 -h,--help                show this help
    --host <url>          Server URL
    --init-keys           Ensure keys exist and exit
    --non-recursive       Do not scan direcories recursively
    --only-verified       Only download entries from verified uploaders
    --verbose             Verbose output
```

#### Environment

For wine to use the shared caches you should set DXVK_STATE_CACHE_PATH as a variable and point it to either:
- `$XDG_CACHE_HOME/dxvk-cache-pool` will work for most people
- or `c:/dxvk-cache-pool` if you did sandbox your wine prefix (`winetricks sandbox`) as in that case wine can't access your home directory. You need to run `dxvk-cache-client` against all your wine prefixes in this case.

It doesn't affect steam/proton, proton is doing it's own thing and overrides that variable.


##### The convenient way

Set it up globally.

See [dxvk-cache-pool.sh](dxvk-cache-pool.sh) for an example you can put directly into `/etc/profile.d/`. The arch package already includes it.


##### The hard way

If you don't want to set it up globally you have to set it before running wine, otherwise it won't use the shared caches.

You can probably configure it in Lutris for the wine prefix you want to use or create a wrapper script.


##### Why this is necessary

The client will create a symlink inside each wine prefix it encounters when scanning from `drive_c/dxvk-cache-pool` to `$XDG_CACHE_HOME/dxvk-cache-pool`.

All caches will be written to $XDG_CACHE_HOME/dxvk-cache-pool, 
so if your wine prefix is missing that symlink or the DXVK_STATE_CACHE_PATH isn't set DXVK won't find the cache.


#### Example

Assuming you store your wine prefixes in `/usr/local/games/wine`, you can run it like:

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

You can pass multiple directories. The directories should contain wine prefixes.
It will search for exe files in the passed directories and automatically update the .dxvk-cache's for you.


### Server
```bash
$ ./dxvk-cache-server -h
usage: dvxk-cache-server [-h] [--port <port>] [--storage <path>]
       [--versions <version>]
 -h,--help                 show this help
    --port <port>          Server port
    --storage <path>       Storage path
    --versions <version>   DXVK state cache versions to accept
```

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


## Implementation problems

### Identifying a game

Possible Solutions:

- Just the exe's filename. After a bit of discussion the only possible choice: https://github.com/doitsujin/dxvk/issues/677
- ~~SHA1 of the exe.~~ Don't want to loose the cache if the application is updated. Games built using an engine can have the same exact binary.
- ~~Steam game id.~~ The most robust and my preferred solution, but would make it exclusive to Steam.
- ~~Exe name plus parent directory.~~ ~~Still suboptimal but right now what I opted for. Assumes users don't go around changing the installation folder name. Should work well for Steam.~~




