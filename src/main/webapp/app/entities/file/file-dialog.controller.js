(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('FileDialogController', FileDialogController);

    FileDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'File', 'User'];

    function FileDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, File, User) {
        var vm = this;

        vm.file = entity;
        vm.clear = clear;
        vm.save = save;
        vm.users = User.query();

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.file.id !== null) {
                File.update(vm.file, onSaveSuccess, onSaveError);
            } else {
                File.save(vm.file, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('visualizer2App:fileUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
