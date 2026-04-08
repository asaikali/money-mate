#!/bin/sh
cd "$(dirname "$0")/.."
./mvnw spring-boot:run -q -pl envoy-egress-gateway
