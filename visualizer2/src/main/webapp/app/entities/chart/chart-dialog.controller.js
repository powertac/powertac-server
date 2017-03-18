(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('ChartDialogController', ChartDialogController);

    ChartDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Chart', 'User', 'Graph'];

    function ChartDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Chart, User, Graph) {
        var vm = this;

        vm.chart = entity;
        vm.clear = clear;
        vm.save = save;
        vm.users = User.query();
        vm.graphs = Graph.query();

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.chart.id !== null) {
                Chart.update(vm.chart, onSaveSuccess, onSaveError);
            } else {
                Chart.save(vm.chart, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('visualizer2App:chartUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
