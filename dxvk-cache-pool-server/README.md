# DXVK cache pool server

Server (users do not need this):
- Centralized storage. Provides REST interface to access caches and signatures.

## Usage

The server requires Java >= 8.

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
