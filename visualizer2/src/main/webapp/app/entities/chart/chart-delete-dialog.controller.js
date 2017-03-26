(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('ChartDeleteController',ChartDeleteController);

    ChartDeleteController.$inject = ['$uibModalInstance', 'entity', 'Chart'];

    function ChartDeleteController($uibModalInstance, entity, Chart) {
        var vm = this;

        vm.chart = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Chart.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
