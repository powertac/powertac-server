# Visualizer

## Introduction

The Visualizer is a web front-end for game display and analysis. It can be built in two flavors.

1. The default build produces the `research` version, which runs embedded with the PowerTAC simulation server. It includes a simple web page for setting up and running individual games.

2. The `tournament` version is build by including `-Ptournament` in the mvn command line. It runs as a separate webapp, under control of the Tournament Manager. It runs continuously, repeatedly asking the TM for a sim session to display. Once the TM gives it the details on a sim, it connects to the sim's visualizer output queue and displays the results.

## Getting Started: embedded version

Unless you want to work with visualizer source, you can run a release or snapshot by using the `-Pweb` maven option in server-distribution. In a source environment, you can build the visualizer "skinny war" using `mvn clean install` and run it with `mvn -Pweb` in server-distribution. 
If everything works well, the message "Jetty Server started" will appear in your console output. Visualizer will be located at `localhost:8080/visualizer/`.

## Getting Started: tournament version

The tournament version is a web-app, typically deployed to a tomcat-7 setup.
It has been tested with apache-tomcat 7.0.27. In a normal multi-machine tournament setup, visualizers typically run on separate machines from the TM and the sims, and you need to install one visualizer for each sim machine. So if you are running sims on four machines, you will need to generate a visualizer .war file, and deploy it in four separate web contexts. It is unclear at this time (July 2012) how many visualizer instances can co-exist on a machine. Generate a war file with `mvn -Ptournament clean war:war`. This will generate a file visualizer.war in the target directory.

To deploy a war file, you typically need to be logged into the machine that's hosting the tomcat instance, because unless you do some tomcat config work, the "manager" functions are restricted to clients on localhost. You need to be in a dir containing the war file and the pom.xml file. The command is

```
    mvn -Ptournament tomcat7:deploy -DsimNode=sim-node -DsimHost=sim-hostname -DtournamentUrl=tmUrl
```

where `sim-hostname` is the hostname of the sim machine this visualizer is supporting, sim-node is the name of the node in the jenkins setup, and `tournamentUrl` is the root context of the tournament manager, typically `http://host:8080/TournamentScheduler`.

You need to do this once for each sim-node. Once deployed, a visualizer running on machine called `viz.powertac.org` for sim-node called `powertac` on a host `sim.powertac.org` would be located at `http://viz.powertac.org:8080/visualizer/powertac`.
