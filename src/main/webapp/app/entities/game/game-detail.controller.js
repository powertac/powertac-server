(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GameDetailController', GameDetailController);

    GameDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'entity', 'Game', 'User', 'File'];

    function GameDetailController($scope, $rootScope, $stateParams, entity, Game, User, File) {
        var vm = this;
        vm.game = entity;
        
        var unsubscribe = $rootScope.$on('visualizer2App:gameUpdate', function(event, result) {
            vm.game = result;
        });
        $scope.$on('$destroy', unsubscribe);

    }
})();
