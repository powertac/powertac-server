(function() {
  'use strict';

  angular
    .module('visualizer2App')
    .config(stateConfig);

  stateConfig.$inject = ['$stateProvider'];

  function stateConfig ($stateProvider) {
    $stateProvider.state('games', {
      parent: 'app',
      url: '/games',
      data: {
        authorities: ['ROLE_USER'],
        pageTitle: 'Games'
      },
      views: {
        'content@': {
          templateUrl: 'app/games/games.html',
          controller: 'GamesController',
          controllerAs: 'vm'
        }
      }
    })
    .state('games.filepicker', {
      parent: 'games',
      url: '/filepicker/{fileType:string}',
      data: {
        authorities: ['ROLE_USER']
      },
      params: {
        fileType: ''
      },
      onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
        $uibModal.open({
          templateUrl: 'app/games/games-filepicker-dialog.html',
          controller: 'GamesFilePickerController',
          controllerAs: 'vm',
          size: 'lg',
          resolve: {
            fileType: function() { return $stateParams.fileType; }
          }
        }).result.then(function(file) {
          $state.go('games');
        }, function() {
          $state.go('^');
        });
      }]
    });
  }

})();
