(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GraphDeleteController',GraphDeleteController);

    GraphDeleteController.$inject = ['$uibModalInstance', 'entity', 'Graph'];

    function GraphDeleteController($uibModalInstance, entity, Graph) {
        var vm = this;
        vm.graph = entity;
        vm.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
        vm.confirmDelete = function (id) {
            Graph.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        };
    }
})();
