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

The server is configured by command-line options and by a
configuration file. Note that a number of these options refer to the
Tournament Scheduler, which is not yet released. See
https://github.com/powertac/powertac-server/wiki/Tournament-Scheduler
for background on this. Also note that the older "script-file"
interface still works - if you were set up to run the 0.1.0 server,
the new version still accepts that style of interface.

The format of the command line depends on the type
of session you want to run. To run a bootstrap session, it's

To run the server in bootstrap mode, the command is

  mvn exec:exec -Dexec.args="--boot bootstrap-data [options]"

where

bootstrap-data is the name (not a URL) of the xml file that will be 
  written with the results of the bootstrap run,

options include:

  --control controller-url 
  gives the url of the Tournament Scheduler api, from which the server
  can request a configuration and a log-prefix string.

  --config server-config
  gives the URL of a properties file that overrides the standard
  server configuration. If this option is missing and the --control
  option is given, the server configuration is retrieved from
  controller-url/server-config.

  --log-suffix suffix
  gives the root name for the log files, and defaults to "boot"; two
  log files are produced: powertac-suffix.trace and
  powertac-suffix.state. If this option is missing and --control is
  given, the logfile prefix will be retrieved from
  controller-url/log-suffix.

To run the server in sim mode, the command is

  mvn exec:exec -Dexec.args="--sim [options]"

where options include the --config, --log-suffix, and --control
options as in bootstrap mode, as well as

  --boot-data bootstrap-data
  gives the URL of the xml file from which a bootstrap record can be
  read. If this option is missing and the --control option is given,
  then the URL for the bootstrap record will be
  controller-url/bootstrap-data. Note: the server will not start if
  one of these two sources does not produce a valid bootstrap
  dataset.

  --brokers broker,...
  is a comma-separated list of broker usernames that are expected to
  log in to the simulation before it starts. If this option is missing
  and --control is provided, then the broker list will be retrieved
  from controller-url/broker-list.

  --log-suffix suffix
  defaults to "sim" rather than "boot".
  
If you want to override some aspect of server configuration that is
not directly supported by command-line options, you will need to dig
out the default configuration from the source distribution, (it's in
server-main/src/main/resources/server.properties), edit it, and specify
it as the argument to the --config option.

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
Jurica Babic, Antonios Chrysopoulos, Travis Daudelin, Josh Edeen, 
Ryan Finneman, Nguyen Nguyen, Erik Onarheim, Markus Peters, 
Vedran Podobnik, Prashant Reddy, Andreas Symeonidis,
Philippe Souza Moraes Ribeiro, and Konstantina Valogianni
