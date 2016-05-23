(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('ViewDialogController', ViewDialogController);

    ViewDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'View', 'User', 'Chart'];

    function ViewDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, View, User, Chart) {
        var vm = this;
        vm.view = entity;
        vm.users = User.query();
        vm.charts = Chart.query();

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        var onSaveSuccess = function (result) {
            $scope.$emit('visualizer2App:viewUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        };

        var onSaveError = function () {
            vm.isSaving = false;
        };

        vm.save = function () {
            vm.isSaving = true;
            if (vm.view.id !== null) {
                View.update(vm.view, onSaveSuccess, onSaveError);
            } else {
                View.save(vm.view, onSaveSuccess, onSaveError);
            }
        };

        vm.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
    }
})();
