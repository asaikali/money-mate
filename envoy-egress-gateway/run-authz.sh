#!/bin/sh
cd "$(dirname "$0")/authz-server"
go run . -config ./config.yaml
