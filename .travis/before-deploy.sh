#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_f1e1040755a0_key -iv $encrypted_f1e1040755a0_iv -in .travis/codesigning.asc.enc -out codesigning.asc -d
    gpg --fast-import codesigning.asc
fi
