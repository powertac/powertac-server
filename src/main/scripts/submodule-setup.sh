#!/bin/bash
# populate and update submodules
# usage:
#  submodule-setup.sh git-repo-name git-branch-name
#  example: submodule-setup.sh origin master

git submodule init
git submodule update


if [ -z "$1" ]; then
    repo=origin
else
    repo="$1"
fi

if [ -z "$2" ]; then
    branch=master
else
    branch=$2
fi

tag=path
while read lhs eq rhs; do
    if [ $lhs = $tag ]; then
	#echo $lhs $eq $rhs
	pushd $rhs
	git checkout $branch
	git pull $repo $branch
	popd
    fi
done < .gitmodules