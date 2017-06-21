(function() {
  'use strict';

  angular
    .module('visualizer2App')
    .controller('GamesController', GamesController);

  GamesController.$inject = ['$log', 'AlertService', 'State', 'Games'];

  function GamesController ($log, AlertService, State, Games) {
    var vm = this;

    vm.setMode = setMode;
    vm.setGameName = setGameName;
    vm.addBroker = addBroker;
    vm.removeBroker = removeBroker;
    vm.getFile = getFile;
    vm.clearFile = clearFile;
    vm.setSource = setSource;
    vm.setReplayUrl = setReplayUrl;
    vm.start = start;
    vm.stop = stop;
    vm.isStartButtonDisabled = isStartButtonDisabled;
    vm.isStopButtonDisabled = isStopButtonDisabled;
    vm.state = State;

    setMode('');

    function setMode (mode) {
      vm.mode = mode;
      vm.gameName = '';
      vm.gameNameTemp = '';
      vm.overwrite = false;
      vm.brokerNames = '';
      vm.brokers = {};
      vm.brokerCollection = [];
      vm.games = [];
      setSource(null);

      Games.reset();
    }

    function setGameName (name) {
      if (name === undefined) {
        vm.gameName = vm.gameNameTemp;
      } else {
        vm.gameName = vm.gameNameTemp = name;
      }
    }

    function addBroker (name) {
      if (!vm.brokers.hasOwnProperty(name)) {
        vm.brokers[name] = true;
        vm.brokerName = null;
        vm.brokerCollection = Object.keys(vm.brokers);
      } else {
        AlertService.error('Broker ' + name + ' was already added');
      }
    }

    function removeBroker (name) {
      if (vm.brokers.hasOwnProperty(name)) {
        delete vm.brokers[name];
        vm.brokerCollection = Object.keys(vm.brokers);
      } else {
        AlertService.error('Broker ' + name + ' was not added');
      }
    }

    function getFile (type) {
      return Games[type + 'File'];
    }

    function clearFile (type) {
      return Games[type + 'File'] = null;
    }

    function setSource (source) {
      vm.replaySource = source;
      vm.replayUrl = '';
      vm.replayUrlTemp = '';
      vm.replayGame = null;
    }

    function setReplayUrl (url) {
      if (url === undefined) {
        vm.replayUrl = vm.replayUrlTemp;
      } else {
        vm.replayUrl = vm.replayUrlTemp = url;
      }
    }

    function start () {
      if (vm.mode === 'BOOT') {
        startBoot();
      } else if (vm.mode === 'SIM') {
        startSim();
      } else if (vm.mode === 'REPLAY') {
        startReplay();
      } else {
        $log.error('Unknown game mode selected: ' + vm.mode);
      }
    }

    function stop () {
      if (State.gameStatus === 'RUNNING' || State.gameStatus === 'WAITING') {
        stopGame();
      } else {
        $log.error('Can\'t stop game, doesn\'t seem to be anything running');
      }
    }

    function isStartButtonDisabled () {
      return !isGameValid() || !State.gameStatus || State.gameStatus === 'RUNNING' || State.gameStatus === 'OFFLINE';
    }

    function isStopButtonDisabled () {
      return State.gameStatus !== 'RUNNING' && State.gameStatus !== 'WAITING';
    }

    // HELPER FUNCTIONS

    function isGameValid () {
      if (!vm.mode) {
        return false;
      } else if (vm.mode === 'SIM' && vm.gameName && Games.bootFile && vm.brokerCollection.length > 0) {
        return true;
      } else if (vm.mode === 'BOOT' && vm.gameName) {
        return true;
      } else if (vm.mode === 'REPLAY' && vm.replaySource && (Games.stateFile || vm.replayUrl)) {
        return true;
      }
      return false;
    }

    function startBoot () {
      vm.setGameName();
      var game = {
        id: null,
        name: vm.gameName,
        type: 'BOOT',
        shared: false,
        completed: false,
        seedFile : Games.seedFile,
        configFile : Games.configFile,
        weatherFile : Games.weatherFile
      };
      Games.boot(game, vm.overwrite,
        function () {
          setMode('');
        },
        function (error) {
          console.log('Failed boot', error);
        }
      );
    }

    function startSim () {
      vm.setGameName();
      var game = {
        id: null,
        name: vm.gameName,
        type: 'SIM',
        shared: false,
        completed: false,
        bootFile : Games.bootFile,
        seedFile : Games.seedFile,
        configFile : Games.configFile,
        weatherFile : Games.weatherFile,
        brokers: vm.brokerCollection.join(',')
      };
      Games.run(game, vm.overwrite,
        function () {
          setMode('');
        },
        function (error) {
          console.log('Failed sim', error);
        }
      );
    }

    function startReplay () {
      if  (vm.replaySource === 'EXTERNAL') {
        Games.replayExternal(vm.replayUrl,
            function () {
              setMode('');
            },
            function (error) {
              console.error('Failed replay', error);
            }
        );
      } else {
        Games.replayInternal(Games.stateFile,
            function () {
              setMode('');
            },
            function (error) {
              console.error('Failed replay', error);
            }
        );
      }
    }

    function stopGame () {
      Games.close(
          function() {
            setMode('');
            setSource('');
          },
          function (error) {
            console.error('Failed stop', error);
          }
      );
    }
  }

})();
