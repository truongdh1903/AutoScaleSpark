#!/bin/bash

set -euo pipefail
[ -n "${DEBUG:-}" ] && set -x

/etc/init.d/ssh start
# hdfs namenode -format

cd /opt/hadoop
start-all.sh

tail -f /dev/null
exec $@