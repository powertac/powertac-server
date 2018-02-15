# Power TAC Simulation Server

[![Join the chat at https://gitter.im/powertac/powertac-server](https://badges.gitter.im/powertac/powertac-server.svg)](https://gitter.im/powertac/powertac-server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Introduction

Power TAC is a competitive simulation designed to support the study of retail electric power markets, especially as they relate to managing distributed renewable power sources. For more information, see http://www.powertac.org.

## Getting Started 



Go to the Power TAC developer's wiki at
https://github.com/powertac/powertac-server/wiki for information on design, development, and deployment of the Power TAC simulation server.

## PowerTAC Continuous Integration Server

The server is built and tested on a regular basis on the [Minnesota jenkins server](http://tac04.cs.umn.edu:8080/).

## Module structure

This is a multi-module structure, using maven for dependency management and build automation, and using git submodules to tie the pieces together and help create the correct directory structure in a development environment. This module is the root. It contains the parent pom, and the core server module. Other modules include [server-interface](https://github.com/powertac/server-interface), [accounting](https://github.com/powertac/accounting), [auctioneer](https://github.com/powertac/auctioneer), [default-broker](https://github.com/powertac/default-broker), [distribution-utility](https://github.com/powertac/distribution-utility), [genco](https://github.com/powertac/genco), and [household-customer](https://github.com/powertac/household-customer).

To populate a development environment, simply clone this repo, then populate the submodules. You can do this with `submodule init` followed by `submodule update` (the [Pro Git](http://progit.org/book/ch6-6.html) book has a good overview), or you can use the script `src/main/scripts/submodule-setup.sh`, which lets you specify a remote and branch (typically `origin` and `master`). Note that the submodule definitions are contained in the file `.gitmodules` that the current version of that file contains read-only urls for the modules, and that those may be out-of-date. If you plan to work on the code, you can simply change the URL for an individual module, as in `git remote set-url origin git@github.com:powertac/auctioneer.git`.

To update your development environment, you can always do

` git submodule foreach git pull origin master`

Note that some modules, such as common and the Sample Broker, are not submodules of powertac-server. If you need sources for those, you will need to pull them down separately.
