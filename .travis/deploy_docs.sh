#!/usr/bin/env bash

if [[ $TRAVIS_PULL_REQUEST == false && ($TRAVIS_BRANCH == $TRAVIS_TAG || $TRAVIS_BRANCH == "master") ]]
then
  mvn -nsu -B javadoc:aggregate
  git clone -b gh-pages https://github.com/$TRAVIS_REPO_SLUG.git dox
  cd dox
  mkdir -p $TRAVIS_BRANCH
  cp -r $TRAVIS_BUILD_DIR/target/site/apidocs/* $TRAVIS_BRANCH

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
  git push https://erikkemperman:$GITHUB_TOKEN@github.com/$TRAVIS_REPO_SLUG.git
fi
