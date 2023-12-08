#!/usr/bin/env bash

RAMP_USERS=200000
DURING=60

mvn gatling:test \
  -Dsut.host=$SUT_HOST \
  -Dsut.port=$SUT_PORT \
  -Dsut.use.h2c=$SUT_H2C \
  -Dbase.get.ramp.users=$RAMP_USERS \
  -Dbase.get.during=$DURING \
  -Dbase.post.ramp.users=$RAMP_USERS \
  -Dbase.post.during=$DURING \
  -Dbase.post.file.ramp.users=$RAMP_USERS \
  -Dbase.post.file.during=$DURING