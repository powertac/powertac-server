December 2011

Welcome to the 0.1.0 release of the Power TAC server. This is a
limited release, containing the server only. It is intended to support
broker development and simple experiments.

Release notes are available at
http://www.powertac.org/wiki/index.php/Getting_Started.

Javadocs are available at
http://tac04.cs.umn.edu:8080/job/powertac-server/ws/target/site/api/html/index.html.

Running the server
------------------
The server is distributed as a maven pom.xml file. The first time you
run it, maven will download all the server components, as well as
other libraries, from Maven Central (or from the Sonatype snapshot
repository if you are running a snapshot version). This can take some
time the first time you start the server.

Before you run the server, note that it runs in two different modes:

- in bootstrap mode, only the "default" broker is active, and
  all customers are subscribed to its simple production and
  consumption tariffs. The bootstrap period is typically 360 timeslots
  (15 days), and data from the last 14 days is collected and used to
  "seed" a normal simulation run.

- in sim mode, competing brokers are allowed to log in. Before the sim
  starts, the bootstrap dataset is broadcast to all brokers to permit
  them to seed their models, such as customer usage profiles and
  wholesale market price models. The simulation then starts at a point
  in simulated time immediately following the end of the bootstrap
  period. Many sims can be run with the same bootstrap dataset.

The server is configured by a simple script file; each line specifies
either a bootstrap run or a sim run, along with an optional
configuration file and (for sim runs) a list of authorized brokers.
Each line of the script is in one of two forms:
  bootstrap [--config boot-config.properties]
  sim [--config sim-config.properties] broker ...
where

- boot-config.properties and sim-config.properties are the names of
  properties files that specify the server setup. A sample properties
  file is provided in config/test.properties. The commented-out
  properties show the default values. Note that some properties, such
  as the length of the bootstrap session, are set in a bootstrap
  session and cannot be overridded in a sim session.

- broker ... is a space-separated list of broker usernames. Once the
  sim starts up and initializes itself, it will stall until exactly
  those brokers have logged in. Logins from other brokers will be
  rejected; logins prior to the start of the initialization process
  will be ignored.

To run the server with the script file config/bootstrap.txt, the command is

  mvn exec:java -Dexec.args="config/bootstrap.txt"

Access to code resources
------------------------

The server and other project assets are archived at
https://github.com/powertac. The developer's wiki is at
https://github.com/powertac/powertac-server/wiki/.
You may wish to subscribe to the developer's mailing list at
http://power-tac-developers.975333.n3.nabble.com//

Bug reporting
-------------

If you believe you have found a bug and can describe it with some
degree of accuracy, you are welcome to create an issue at
https://github.com/powertac/powertac-server/issues. If you have a
question or a suggestion for the development team, you are welcome to
post a message to the developer's mailing list. Keep in mind that the
development team is entirely composed of volunteers and students, and
although we will do our best to respond in a timely fashion, response
will not always be immediate.

Please let us know what you think of the Power TAC system, and how we
can improve our software and processes.

John Collins, Wolf Ketter, and the Power TAC development team:
Jurica Babic, Antonios Chrysopoulos, Travis Daudelin, David Dauer,
Josh Edeen, Ryan Finneman, Chris Flath, Adis Mustedanagic, Nguyen
Nguyen, Erik Onarheim, Markus Peters, Prashant Reddy, Philippe Souza
Moraes Ribeiro, Daniel Schnurr, and Konstantina Valogianni
