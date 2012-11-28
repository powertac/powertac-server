# Visualizer

## Introduction

The Visualizer is a web front-end for game display and analysis.
The ultimate goal is to integrate the Visualizer with Tournament scheduler,
but until that you can try it out with some default configuration, see Getting started for more info.

* The `embedded` version runs embedded with the PowerTAC simulation server.
  It includes a simple web page for setting up and running individual games.
* The `tournament` version runs as a separate webapp, under control of the Tournament Manager.
  It runs continuously, repeatedly asking the TM for a sim session to display.
  Once the TM gives it the details on a sim, it connects to the sim's visualizer output queue and displays the results.

## Getting Started: embedded version

To run the Visualizer in embedded mode, first you will have to install all of the PowerTAC modules.
Go to the Power TAC developer's wiki at
https://github.com/powertac/powertac-server/wiki for information on design, development, and deployment of the Power TAC simulation server.

Clone the Visualizer from git repository (preferably in the same directory where powertac-server and common were cloned).
In this directory, compile the visualizer with `mvn clean compile`.

To run the visualizer, use maven as `mvn jetty:run` inside your visualizer folder.
If everything works well, the message "Jetty Server started" will appear in your console output.
Visualizer is located at `localhost:8080/visualizer/`.

## Getting Started: tournament version

The tournament version is a web-app, typically deployed to a tomcat-7 setup.
It has been tested with apache-tomcat 7.0.27.
In a normal multi-machine tournament setup, visualizers typically run on separate machines from the TM and the sims,
and you install one visualizer for each sim machine.
So if you are running sims on four machines, you will need to generate a visualizer .war file, and deploy it in four separate web contexts.
It is unclear at this time (July 2012) how many visualizer instances can co-exist on a machine.

Generate a war file with `mvn clean war:war`. This will generate a file visualizer.war in the current directory.

To deploy a war file, you typically need to be logged into the machine that's hosting the tomcat instance,
because unless you do some tomcat config work, the "manager" functions are restricted to clients on localhost.
You need to be in a dir containing the war file and the pom.xml file. The command is

```
    mvn tomcat7:deploy -DsimNode=sim-node -DsimHost=sim-hostname -DtournamentUrl=tmUrl
```

where `sim-hostname` is the hostname of the sim machine this visualizer is supporting,
sim-node is the name of the node in the jenkins setup,
and `tournamentUrl` is the root context of the tournament manager, typically `http://host:8080/TournamentScheduler`.

You need to do this once for each sim-node.
Once deployed, a visualizer running on machine called `viz.powertac.org` for sim-node called `powertac` on a host `sim.powertac.org`
would be located at `http://viz.powertac.org:8080/visualizer/powertac`.
