#!/bin/bash

SCRIPT="$0"
SCRIPT_DIR=`dirname $SCRIPT`

# TODO: (optional?) maven build task

# Start server
exec "$SCRIPT_DIR"/virgo-kernel/bin/startup.sh
