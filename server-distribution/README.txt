April 2012

Welcome to the 0.5.0 release of the Power TAC server. This release
contains the server and an initial version of the game visualizer,
including a simple control panel that allows you to set up and run
bootstrap and competition sessions. There is a compatible sample
broker distributed separately. This release is intended to support
broker development and simple experiments.

Release notes are available at
http://www.powertac.org/wiki/index.php/Getting_Started.

Javadocs are available at
http://tac04.cs.umn.edu:8080/job/powertac-server/ws/target/site/api/html/index.html.

Running the server
------------------
The server is distributed as a maven pom.xml file, and you must have
Apache Maven installed to use it. The first time you
run it, maven will download all the server components, as well as
other libraries, from Maven Central (or from the Sonatype snapshot
repository if you are running a snapshot version). This can take some
time the first time you start the server. If you are running the web
version, the main download file is over 26 Mb.

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

The server can be run from the command line (the "cli" option), and
from the web interface (the "web" option). Both options accept the
same input settings, except that the web option does not support the
Tournament Manager.

Configuration is by command-line options or the equivalent data in a
web form, and by a configuration file. Note that a number of these
options refer to the Tournament Manager, which is not yet
released. See
https://github.com/powertac/powertac-server/wiki/Tournament-Scheduler
for background on this.

The command line options depend on the type of session you want to
run. To run a bootstrap session, the command is

  mvn -Pcli -Dexec.args="--boot bootstrap-data [options]"

where

  bootstrap-data is the name (not a URL) of the xml file that will be 
  written with the results of the bootstrap run,

options include:

  --control controller-url 
  gives the URL of the Tournament Manager api, from which the server
  can request a configuration and a log-prefix string.

  --config server-config
  gives the URL (or a filename) of a properties file that overrides
  the standard server configuration. If this option is missing and
  the --control option is given, the server configuration is retrieved
  from controller-url/server-config.

  --log-suffix suffix
  gives the root name for the log files, and defaults to "boot"; two
  log files are produced: powertac-suffix.trace and
  powertac-suffix.state. If this option is missing and --control is
  given, the logfile prefix will be retrieved from
  controller-url/log-suffix.

To run the server from the command line in sim mode, the command is

  mvn -Pcli -Dexec.args="--sim [options]"

where options include the --config, --log-suffix, and --control
options as in bootstrap mode, as well as

  --boot-data bootstrap-data
  gives the URL (or simply a filename) of the xml file from which a
  bootstrap record can be read. If this option is missing and the
  --control option is given, then the URL for the bootstrap record
  will be controller-url/bootstrap-data. Note that the server will not
  start if one of these two sources does not produce a valid bootstrap
  dataset.

  --jms-url url
  gives the URL of the jms message broker, which is typically, but not
  necessarily, instantiated inside the server. The default value is
  tcp://localhost:61616 unless you change it in your server
  configuration file. If you want to connect to it from another
  host, you need to use a valid hostname rather than localhost, and
  the brokers must specify the same URL.

  --brokers broker,...
  is a comma-separated list (no whitespace allowed) of broker
  usernames that are expected to log in to the simulation before it
  starts. If this option is missing and --control is provided, then
  the broker list will be retrieved from controller-url/broker-list.
  A broker name can be given as username/queue-name, in which case the
  broker's input queue will be called queue-name. If the queue-name is
  not given, then the broker's input queue name will be the same as
  its username.

  --input-queue name
  gives the name of the jms input queue for the server. If not given,
  then the jms input queue is called 'serverInput'.

  --log-suffix suffix
  defaults to "sim" rather than "boot".
  
If you want to override some aspect of server configuration that is
not directly supported by command-line options, you will need to edit
the sample server configuration file given in
config/server.properties, and then specify it as the argument to the
--config option.

To run the server under control of the visualizer, the command is
simply

  mvn -Pweb

Once it is running (it will print '[INFO] Started Jetty Server'),
browse to http://localhost:8080/visualizer and navigate to the
Competition Control page. There you will see a web form that allows
you to fill in the same options accepted by the cli version. Once a
sim session is running, the Game Status message will change to
"Running", and you can then navigate to the Game View. Note that it is
not necessary to shut down the web server (which you do by aborting
the maven process with Control-C) between sessions.

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

John Collins, Wolf Ketter, and the Power TAC development team: Jurica
Babic, Mehdi Benyebka, Antonios Chrysopoulos, Travis Daudelin, Mathijs
de Weerdt, Josh Edeen, Ryan Finneman, Nguyen Nguyen, Erik Onarheim,
Shashank Pande, Markus Peters, Vedran Podobnik, Kailash Ramanathan,
Prashant Reddy, Andreas Symeonidis, Philippe Souza Moraes Ribeiro, and
Konstantina Valogianni
