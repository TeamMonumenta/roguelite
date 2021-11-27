#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"
./compile.sh
cd "$SCRIPT_DIR/target"

plugin="$(ls -r Roguelite*.jar | grep -v sources | head -n 1)"
if [[ -z "$plugin" ]]; then
	exit 1
fi

echo "Plugin version: $plugin"

scp -P 9922 $plugin epic@admin.playmonumenta.com:/home/epic/stage/m12/server_config/plugins
ssh -p 9922 epic@admin.playmonumenta.com "cd /home/epic/stage/m12/server_config/plugins && rm -f Roguelite.jar ; ln -s $plugin Roguelite.jar"
