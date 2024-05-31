#!/usr/bin/env bash

arguments=("$@")

java \
 -server \
 -Xmx1024m \
 -jar "/powertac/server/powertac-server.jar" \
 "${arguments[@]}"
