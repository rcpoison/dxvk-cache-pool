#!/bin/bash
set -e

defaultKey=$(gpgconf --list-options gpg | awk -F: '$1 == "default-key" {print $10}'|cut -c 2-)
[ -z "$defaultKey" ] && { echo "No default key found!"; exit 1; }

if [ -x "dxvk-cache-client" ]; then
  ./dxvk-cache-client --init-keys
else
  dxvk-cache-client --init-keys
fi

mkdir verification-request
(
  cd verification-request
  
  cp $XDG_CONFIG_HOME/dxvk-cache-pool/ec.pub .
  gpg --output ec.pub.sig --detach-sig ec.pub
  gpg --output public.key --export "$defaultKey"
)

tar -c verification-request|gzip -9> verification-request.tar.gz

rm verification-request/{ec.pub,ec.pub.sig,public.key}
rmdir verification-request

echo "Please create a ticket and attach verification-request.tar.gz"
