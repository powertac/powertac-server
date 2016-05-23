(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('ViewDeleteController',ViewDeleteController);

    ViewDeleteController.$inject = ['$uibModalInstance', 'entity', 'View'];

    function ViewDeleteController($uibModalInstance, entity, View) {
        var vm = this;
        vm.view = entity;
        vm.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
        vm.confirmDelete = function (id) {
            View.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        };
    }
})();
