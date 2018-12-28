#!/usr/bin/env bash

if [[ $TRAVIS_PULL_REQUEST == false && ($TRAVIS_BRANCH == $TRAVIS_TAG || $TRAVIS_BRANCH == "master") ]]
then
  mvn -nsu -B site
  mvn -nsu -B site:stage

  git clone https://github.com/powertac/powertac.github.io.git dox
  cd dox/server
  mkdir -p $TRAVIS_BRANCH
  cp -r $TRAVIS_BUILD_DIR/target/staging/powertac-server/* $TRAVIS_BRANCH

  for branch in $(ls | grep -v index)
  do
    cat index.item | sed -e s/ITEM/$branch/g >> index.temp
  done
  cat index.head > index.html
  cat index.temp | sort -u >> index.html
  cat index.foot >> index.html
  rm index.temp

  git add index.html $TRAVIS_BRANCH
  git commit -m "Updated javadocs for branch $TRAVIS_BRANCH"
  git push https://erikkemperman:$GITHUB_TOKEN@github.com/powertac/powertac.github.io.git
fi
