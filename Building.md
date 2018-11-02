## Building

**Prerequisites:**
- maven 3
- openjdk >= 8

**Build:**
```bash
./build.sh
```

**Executables:**
```bash
dxvk-cache-client
dxvk-cache-server
```

**Debian package:**
```
dxvk-cache-pool-client/target/dxvk-cache-pool-client_*_all.deb
```

### Archlinux

You can use the AUR: https://aur.archlinux.org/pkgbase/dxvk-cache-pool-git/

Otherwise see [PKGBUILD](arch/PKGBUILD)

## Implementation problems

### Identifying a game

Possible Solutions:

- Just the exe's filename. After a bit of discussion the only possible choice: https://github.com/doitsujin/dxvk/issues/677
- ~~SHA1 of the exe.~~ Don't want to loose the cache if the application is updated. Games built using an engine can have the same exact binary.
- ~~Steam game id.~~ The most robust and my preferred solution, but would make it exclusive to Steam.
- ~~Exe name plus parent directory.~~ ~~Still suboptimal but right now what I opted for. Assumes users don't go around changing the installation folder name. Should work well for Steam.~~
