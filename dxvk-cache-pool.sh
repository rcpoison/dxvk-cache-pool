#!/bin/bash
#export DXVK_STATE_CACHE_PATH="$XDG_CACHE_HOME/dxvk" # $XDG_CACHE_HOME is not yet available when sourcing this ins /etc/profile.d
export DXVK_STATE_CACHE_PATH="$HOME/.cache/dxvk"

[ ! -d "$DXVK_STATE_CACHE_PATH" ] && mkdir -p "$DXVK_STATE_CACHE_PATH"
