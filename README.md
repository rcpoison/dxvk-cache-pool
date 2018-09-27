# DXVK cache pool

Client/server to share dxvk pipeline cache.

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

## Implementation problems

### Identifying a game

Possible Solutions:

- ~~SHA1 of the exe.~~ Don't want to loose the cache if the application is updated. Games built using an engine can have the same exact binary.
- ~~Just the exe's filename.~~ I don't know how many ShooterGame.exe and Start.exe are out there.
- ~~Steam game id.~~ The most robust and my preferred solution, but would make it exclusive to Steam.
- Exe name plus parent directory. Still suboptimal but right now what I opted for. Assumes users don't go around changing the installation folder name. Should work well for Steam.


### Security

There is none.

- Anybody can submit entries. Nothing prevents anybody from poisoning the cache. Even authentication and a network of trust would be of little help as:
- Currently there is no way to validate the DxvkStateCacheEntry struct and its members. Doing so would be hard to impossible.

Securing the service would probably require all of the following:
- Implement authentication for submission.
- Require the same cache entry to be submitted by different users before considering it a candidate. Otherwise one could simply submit valid entries from different games and create a huge state cache.
- Parse DxvkStateCacheEntry struct.
- Verify Sha1Hash.
- Validate DxvkStateCacheKey, DxvkGraphicsPipelineStateInfo, DxvkRenderPassFormat?
