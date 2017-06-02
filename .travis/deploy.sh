#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    mvn deploy -P deploy-to-ossrh -DskipTests --settings .travis/travis-settings.xml
fi
