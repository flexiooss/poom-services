#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $0))

export SUT_HOST=localhost
export SUT_PORT=9001

$SCRIPT_DIR/campain.sh