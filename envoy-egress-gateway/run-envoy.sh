#!/bin/sh
cd "$(dirname "$0")"
envoy -c envoy.yaml --log-level info
