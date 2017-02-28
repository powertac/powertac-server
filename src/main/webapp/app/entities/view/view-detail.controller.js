(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('ViewDetailController', ViewDetailController);

    ViewDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'View', 'User', 'Chart'];

    function ViewDetailController($scope, $rootScope, $stateParams, previousState, entity, View, User, Chart) {
        var vm = this;

        vm.view = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('visualizer2App:viewUpdate', function(event, result) {
            vm.view = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
