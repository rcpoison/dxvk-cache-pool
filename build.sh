#!/bin/sh
mvn clean install

cp bin-template.sh dxvk-cache-client
cat dxvk-cache-pool-client/target/dxvk-cache-pool-client-*-boot.jar >> dxvk-cache-client
chmod +x dxvk-cache-client

cp bin-template.sh dxvk-cache-server
cat dxvk-cache-pool-server/target/dxvk-cache-pool-server-*-boot.jar >> dxvk-cache-server
chmod +x dxvk-cache-server

