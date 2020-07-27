# Power TAC Simulation Server

[![Join the chat at https://gitter.im/powertac/powertac-server](https://badges.gitter.im/powertac/powertac-server.svg)](https://gitter.im/powertac/powertac-server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Introduction

Power TAC is a competitive simulation designed to support the study of retail electric power markets, especially as they relate to managing distributed renewable power sources. For more information, see http://www.powertac.org.

## Getting Started 



Go to the Power TAC developer's wiki at
https://github.com/powertac/powertac-server/wiki for information on design, development, and deployment of the Power TAC simulation server.

## PowerTAC Continuous Integration Server

PowerTAC is automatically built by Travis CI on new commits and pull requests. Furthermore, maven reports are generated for
releases (tags) and commits to the master branch. These reports can subsequently be found at

https://powertac.github.io/index.html

These reports include the JavaDocs, at

https://powertac.github.io/master/apidocs/index.html  (replace `master` with a specific tag if you want a particular release).

## Module structure

This is a multi-module structure, using maven for dependency
management and build automation. This module is the root. It contains
the parent pom, which specifies dependencies and builds all the modules. All the source code is in the individual modules in
subdirectories. A maven build at this level will build the
modules in all the subdirectories.
To populate a development environment, simply clone this repo.
To view/modify the code, you can import submodules into your development
environment as separate maven modules.

Note that this module depends on core modules in the [powertac-core](https://github.com/powertac/powertac-core)
repo. The [sample broker](https://github.com/powertac/sample-broker) is also in a separate repo.
