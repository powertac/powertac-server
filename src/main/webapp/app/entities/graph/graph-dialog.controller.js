(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GraphDialogController', GraphDialogController);

    GraphDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Graph', 'User'];

    function GraphDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Graph, User) {
        var vm = this;
        vm.graph = entity;
        vm.users = User.query();

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        var onSaveSuccess = function (result) {
            $scope.$emit('visualizer2App:graphUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        };

        var onSaveError = function () {
            vm.isSaving = false;
        };

        vm.save = function () {
            vm.isSaving = true;
            if (vm.graph.id !== null) {
                Graph.update(vm.graph, onSaveSuccess, onSaveError);
            } else {
                Graph.save(vm.graph, onSaveSuccess, onSaveError);
            }
        };

        vm.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
    }
})();
