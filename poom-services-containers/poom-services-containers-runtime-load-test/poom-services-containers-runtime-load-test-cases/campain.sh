#!/usr/bin/env bash

RAMP_USERS=50000
DURING=60

mvn gatling:test \
  -Dsut.host=$SUT_HOST \
  -Dsut.port=$SUT_PORT \
  -Dbase.get.ramp.users=$RAMP_USERS \
  -Dbase.get.during=$DURING \
  -Dbase.post.ramp.users=$RAMP_USERS \
  -Dbase.post.during=$DURING \
  -Dbase.post.file.ramp.users=$RAMP_USERS \
  -Dbase.post.file.during=$DURING