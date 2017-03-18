(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GraphDetailController', GraphDetailController);

    GraphDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Graph', 'User'];

    function GraphDetailController($scope, $rootScope, $stateParams, previousState, entity, Graph, User) {
        var vm = this;

        vm.graph = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('visualizer2App:graphUpdate', function(event, result) {
            vm.graph = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
