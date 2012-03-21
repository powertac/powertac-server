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
Now go in your browser: localhost:8080/visualizer9/
Navigate to competition control page and poke "Run2" button to start the new comeptition with an embedded bootstrap file.
Simulation will now wait for broker "Sample" to log in.
Finally, go to:
localhost:8080/visualizer9/app/brokers
to see the brokers page.
