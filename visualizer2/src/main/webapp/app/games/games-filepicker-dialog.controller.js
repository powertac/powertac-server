(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GamesFilePickerController', GamesFilePickerController);

    GamesFilePickerController.$inject = ['$scope', '$uibModalInstance', 'fileType', 'Files', 'Games'];

    function GamesFilePickerController($scope, $uibModalInstance, fileType, Files, Games) {
        var vm = this;

        vm.fileType = fileType;
        vm.selected = null;
        vm.itemsPerPage = 10;
        vm.filesCollection = [];

        vm.refresh = refresh;
        vm.cancel = cancel;
        vm.submit = submit;
        vm.select = select;

        vm.refresh();

        var selectTime = -250;
        function select (row) {
            if (vm.selected != null && vm.selected.id === row.id) {
                if ((Date.now() - selectTime) < 250) {
                    submit();
                } else {
                    vm.selected = null;
                }
                selectTime = -250;
            } else {
                vm.selected = row;
                selectTime = Date.now();
            }
        }

        function refresh () {
            vm.filesCollection = Files.query({type: vm.fileType});
        }

        function cancel () {
            $uibModalInstance.dismiss('cancel');
        }

        function submit () {
            Games[vm.fileType + 'File'] = vm.selected;
            $uibModalInstance.close(vm.selected);
        }
    }
})();
