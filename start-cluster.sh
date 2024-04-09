#!/bin/bash

cd /mnt/4E9A200D9A1FF067/University/20202/GR

sudo sysctl -w vm.max_map_count=262144

docker stack deploy -c docker-compose.yml cluster

/opt/elasticsearch/bin/elasticsearch
