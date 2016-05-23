(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('FileDetailController', FileDetailController);

    FileDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'entity', 'File', 'User'];

    function FileDetailController($scope, $rootScope, $stateParams, entity, File, User) {
        var vm = this;
        vm.file = entity;
        
        var unsubscribe = $rootScope.$on('visualizer2App:fileUpdate', function(event, result) {
            vm.file = result;
        });
        $scope.$on('$destroy', unsubscribe);

    }
})();
