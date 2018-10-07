## FAQ

#### Why is setting an environment variable necessary?

The client will create a symlink inside each wine prefix it encounters when scanning from `drive_c/dxvk-cache-pool` to `$XDG_CACHE_HOME/dxvk-cache-pool`.

All caches will be written to $XDG_CACHE_HOME/dxvk-cache-pool, so if your wine prefix is missing that symlink or the DXVK_STATE_CACHE_PATH isn't set DXVK won't find the cache.

#### What if I don't want to set the environment variable globally?

If you don't want to set it up globally you have to set it before running wine, otherwise it won't use the shared caches.

You can probably configure it in Lutris for the wine prefix you want to use or create a wrapper script.
