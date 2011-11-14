#!/bin/bash
# populate and update submodules
# usage:
#  submodule-setup.sh git-repo-name git-branch-name
#  example: submodule-setup.sh origin master

git submodule init
git submodule update

repo=$1
branch=$2
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