#!/usr/bin/env bash

cd $HOME
git clone https://github.com/powertac/powertac-core.git
cd powertac-core

git branch -a | grep -o "origin/$TRAVIS_BRANCH"
if [[ $? == 0 ]]
then
  git checkout origin/$TRAVIS_BRANCH
fi

mvn clean install -B -DskipTests
cd $TRAVIS_BUILD_DIR
