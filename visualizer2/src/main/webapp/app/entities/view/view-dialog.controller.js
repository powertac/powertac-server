(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('ViewDialogController', ViewDialogController);

    ViewDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'View', 'User', 'Chart'];

    function ViewDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, View, User, Chart) {
        var vm = this;

        vm.view = entity;
        vm.clear = clear;
        vm.save = save;
        vm.users = User.query();
        vm.charts = Chart.query();

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.view.id !== null) {
                View.update(vm.view, onSaveSuccess, onSaveError);
            } else {
                View.save(vm.view, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('visualizer2App:viewUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
