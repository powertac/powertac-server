(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('FileDeleteController',FileDeleteController);

    FileDeleteController.$inject = ['$uibModalInstance', 'entity', 'File'];

    function FileDeleteController($uibModalInstance, entity, File) {
        var vm = this;

        vm.file = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            File.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
