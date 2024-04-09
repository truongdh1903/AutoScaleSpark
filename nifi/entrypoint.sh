#!/usr/bin/env bash

set -euo pipefail
[ -n "${DEBUG:-}" ] && set -x

sed -i 's/nifi.web.http.host=127.0.0.1/nifi.web.http.host=0.0.0.0/g' /opt/nifi/conf/nifi.properties

cd /opt/nifi
nifi.sh start

tail -f /dev/null
exec $@