#!/bin/bash

set -euo pipefail
set -xv

exec \
  ./cli \
  -alsologtostderr=1 \
  -stderrthreshold=0 \
  -log_dir=logs \
  dev \
  --disable_sdk_generation=true \
  -local_connection_string='postgresql://postgres:postgres@localhost:5432/emulator?sslmode=disable' \
