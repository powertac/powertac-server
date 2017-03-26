(function() {
  'use strict';

  angular
    .module('visualizer2App')
    .factory('Games', Games);

  Games.$inject = ['$http'];

  function Games ($http) {
    var service = {
      list: list,
      boot: boot,
      run: run,
      replayInternal: replayInternal,
      replayExternal: replayExternal,
      close: close,
      reset: reset
    };
    service.reset();

    function reset () {
      service.bootFile = null;
      service.stateFile = null;
      service.seedFile = null;
      service.configFile = null;
      service.weatherFile = null;
    }

    function list (success, error) {
      console.log('Fetching list');
      $http.get('api/mygames', httpOpts).then(success, error);
    }

    function boot (game, overwrite, success, error) {
      console.log('Starting boot');
      $http.post('api/bootgame?overwrite=' + overwrite, game).then(success, error);
    }

    function run (game, overwrite, success, error) {
      console.log('Starting game');
      $http.post('api/simgame?overwrite=' + overwrite, game).then(success, error);
    }

    function replayInternal (file, success, error) {
      console.log('Starting replay internal');
      $http.post('api/replaygame_internal', file).then(success, error);
    }


    function replayExternal (file, success, error) {
      console.log('Starting replay external');
      $http.post('api/replaygame_external', file).then(success, error);
    }

    function close (success, error) {
      console.log('Closing game');
      $http.post('api/closegame').then(success, error);
    }

    return service;


    var httpOpts = {
      transformResponse: function (data) {
        data = angular.fromJson(data);
        data.date = DateUtils.convertDateTimeFromServer(data.date);
        return data;
      }
    };
  }
})();