#!/bin/bash

# $XDG_CACHE_HOME isn't available here, assume it's set to its default value
export DXVK_STATE_CACHE_PATH="$HOME/.cache/dxvk-cache-pool"

mkdir -p "$DXVK_STATE_CACHE_PATH"
