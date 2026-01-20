#!/bin/bash
echo "Starting Stream Benchmark for BoxCast Proxy..."
echo "Target: https://boxcast-proxy.aswin-c21.workers.dev/trending?limit=50&country=in"

# We use curl with --trace-time to see when bytes arrive
# We filter for the incoming data lines to see the 2+2+4 chunks arriving

curl -N -s --trace-time -v "https://boxcast-proxy.aswin-c21.workers.dev/trending?limit=50&country=in" 2>&1 | grep -E "< |\{" | head -n 30
