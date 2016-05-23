(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('FileDialogController', FileDialogController);

    FileDialogController.$inject = ['$scope', '$stateParams', '$uibModalInstance', 'entity', 'File', 'User'];

    function FileDialogController ($scope, $stateParams, $uibModalInstance, entity, File, User) {
        var vm = this;
        vm.file = entity;
        vm.users = User.query();

        var onSaveSuccess = function (result) {
            $scope.$emit('visualizer2App:fileUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        };

        var onSaveError = function () {
            vm.isSaving = false;
        };

        vm.save = function () {
            vm.isSaving = true;
            if (vm.file.id !== null) {
                File.update(vm.file, onSaveSuccess, onSaveError);
            } else {
                File.save(vm.file, onSaveSuccess, onSaveError);
            }
        };

        vm.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
    }
})();
