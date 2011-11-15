# Power TAC Simulation Server

## Introduction

Power TAC is a competitive simulation to study retail electric power markets, especially as they relate to managing distributed renewable power sources. For more information, see http://www.powertac.org.

## Getting Started 

Go to the Power TAC wiki at
https://github.com/powertac/powertac-server/wiki for information on design, development, and deployment of the Power TAC simulation server.

## PowerTAC Continuous Integration Server

The server is built and tested on a regular basis on the [Minnesota jenkins server](http://tac04.cs.umn.edu:8080/).

## Module structure

This is a multi-module structure, using maven for dependency management and build automation, and using git submodules to tie the pieces together and help create the correct directory structure in a development environment. This module is the root. It contains the parent pom, and the core server module. Other modules include [server-interface](https://github.com/powertac/server-interface), [accounting](https://github.com/powertac/accounting), [auctioneer](https://github.com/powertac/auctioneer), [default-broker](https://github.com/powertac/default-broker), [distribution-utility](https://github.com/powertac/distribution-utility), [genco](https://github.com/powertac/genco), and [household-customer](https://github.com/powertac/household-customer).

To populate a development environment, simply clone this repo, then populate the submodules. You can do this with `submodule init` followed by `submodule update` (the [Pro Git](http://progit.org/book/ch6-6.html) book has a good overview), or you can use the script `src/main/scripts/submodule-setup.sh`, which lets you specify a remote and branch (typically `origin` and `master`). Note that the submodule definitions are contained in the file `.gitmodules` and that the current version of that file contains read-only urls for the modules. If you plan to work on the code, you should first replace .gitmodules with the read-write version that's provided as rw.gitmodules. _PLEASE_ don't commit a version of .gitmodules with read-write urls, because it will seriously mess up jenkins the next time it tries to build the project.