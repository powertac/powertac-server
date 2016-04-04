Power TAC Broker Core
=====================

The sample broker is intended to help broker developers by providing both a foundation that interfaces correctly with the Power TAC infrastructure. This package implements the "core" of a working broker. It should not need to be modified in order to implement a working broker.

The broker's command-line arguments are processed by the core. Using maven, the command-line is

```
  mvn exec:exec [-Dexec.args="options"]
```

where options may include:
* `--jms-url _url_` specifies the url of the server hosting the game the broker will attempt to join. This option overrides the `samplebroker.core.jmsManagementService.jmsBrokerUrl` property in the configuration file.
* `--config _filename_` specifies the name of a file to configure the broker. If this option is not specified, the file `./broker.properties` will be used.
* `--repeat-count _n_` specifies the number of game sessions the broker will attempt to join in before exiting. This allows unattended operation in multi-game tournaments.
* `--repeat-hours _n_` specifies the duration over which the broker will continue to attempt to join games before exiting. This is an alternative to the `repeat-count` option.
* `--queue-name _name_` specifies the name of the server queue that the broker will attempt to connect to. This is needed for testing, but has little practical use in normal operation; the default queue name is the broker's username (specified in the config file), and for tournament operation the broker gets its queue name from the tournament server, as specified by the `
samplebroker.core.powerTacBroker.tourneyUrl` in the configuration file.
* `--server-queue _name_` specifies the name of the server's incoming queue. Should be used only for testing. In tournament operation, this property is retrieved from the tournament server.
* `--interactive` if given, causes the broker to attempt to pause the server in each timeslot. Useful for development, ignored in tournament mode.