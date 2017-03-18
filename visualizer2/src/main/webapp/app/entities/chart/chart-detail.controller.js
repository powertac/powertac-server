(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('ChartDetailController', ChartDetailController);

    ChartDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Chart', 'User', 'Graph'];

    function ChartDetailController($scope, $rootScope, $stateParams, previousState, entity, Chart, User, Graph) {
        var vm = this;

        vm.chart = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('visualizer2App:chartUpdate', function(event, result) {
            vm.chart = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
