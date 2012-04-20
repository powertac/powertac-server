# Visualizer

## Introduction

The Visualizer is a web front-end for game display and analysis. The ultimate goal is to integrate the Visualizer with Tournament scheduler, but until that you can try it out with some default configuration, see Getting started for more info. 

## Getting Started 

In order to run the Visualizer without Tournament scheduler, first you will have to install all of the PowerTAC modules.
Go to the Power TAC developer's wiki at
https://github.com/powertac/powertac-server/wiki for information on design, development, and deployment of the Power TAC simulation server.

Clone the Visualizer from git repository (preferably in the same directory where powertac-server and common were cloned).

Poke:
'cd visualizer'
'mvn install'

## How to run the Visualizer without Tournament scheduler

To run Visualizer, poke 'mvn jetty:run' inside your visualizer folder.
If everything works well, the message "Jetty Server started" will appear in your console output.

Visualizer is located on the following link:
localhost:8080/visualizer/
