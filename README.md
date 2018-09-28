# DXVK cache pool (WIP)

Client/server to share DXVK pipeline caches.

Client:
- Fetches missing DxvkStateCacheEntry's and patches the .dxvk-cache.
- Submits local DxvkStateCacheEntry's not present on server.

Server:
- Provides REST interface to access caches.

## Building

Prerequisites:
- maven 3
- openjdk>=8

Build: 
```bash
./build.sh
```

Executables:
```bash
dxvk-cache-client
dxvk-cache-server
```

## Usage

```bash
$ ./dxvk-cache-client -h
usage: dxvk-cache-client  directory... [-h] [--host <url>]
 -h,--help         show this help
    --host <url>   Server URL
```

```bash
$ ./dxvk-cache-server -h
usage: dvxk-cache-server [-h] [--port <port>] [--versions <version>]
 -h,--help                 show this help
    --port <port>          Server port
    --versions <version>   DXVK state cache versions to accept
```

## Implementation problems

### Identifying a game

Possible Solutions:

- ~~SHA1 of the exe.~~ Don't want to loose the cache if the application is updated. Games built using an engine can have the same exact binary.
- ~~Just the exe's filename.~~ I don't know how many ShooterGame.exe are out there.
- ~~Steam game id.~~ The most robust and my preferred solution, but would make it exclusive to Steam.
- Exe name plus parent directory. Still suboptimal but right now what I opted for. Assumes users don't go around changing the installation folder name. Should work well for Steam.


### Security

There is none.

- Anybody can submit entries. Nothing prevents cache poisoning. Even authentication and a network of trust would be of little help as:
- Currently there is no way to validate the DxvkStateCacheEntry struct and its members. Doing so would be hard to impossible.

