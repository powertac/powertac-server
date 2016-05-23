(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GraphDetailController', GraphDetailController);

    GraphDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'entity', 'Graph', 'User'];

    function GraphDetailController($scope, $rootScope, $stateParams, entity, Graph, User) {
        var vm = this;
        vm.graph = entity;
        
        var unsubscribe = $rootScope.$on('visualizer2App:graphUpdate', function(event, result) {
            vm.graph = result;
        });
        $scope.$on('$destroy', unsubscribe);

    }
})();
