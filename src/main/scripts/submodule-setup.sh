#!/bin/bash
# populate and update submodules
# usage:
#  submodule-setup.sh git-branch-name

git submodule init
git submodule update

branch=$1
tag=path
while read lhs eq rhs; do
    if [ $lhs = $tag ]; then
	#echo $lhs $eq $rhs
	pushd $rhs
	git checkout $branch
	git pull
	popd
    fi
done < .gitmodules