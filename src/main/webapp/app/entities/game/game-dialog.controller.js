(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GameDialogController', GameDialogController);

    GameDialogController.$inject = ['$scope', '$stateParams', '$uibModalInstance', 'entity', 'Game', 'User', 'File'];

    function GameDialogController ($scope, $stateParams, $uibModalInstance, entity, Game, User, File) {
        var vm = this;
        vm.game = entity;
        vm.users = User.query();
        vm.files = File.query();

        var onSaveSuccess = function (result) {
            $scope.$emit('visualizer2App:gameUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        };

        var onSaveError = function () {
            vm.isSaving = false;
        };

        vm.save = function () {
            vm.isSaving = true;
            if (vm.game.id !== null) {
                Game.update(vm.game, onSaveSuccess, onSaveError);
            } else {
                Game.save(vm.game, onSaveSuccess, onSaveError);
            }
        };

        vm.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };

        vm.datePickerOpenStatus = {};
        vm.datePickerOpenStatus.date = false;

        vm.openCalendar = function(date) {
            vm.datePickerOpenStatus[date] = true;
        };
    }
})();
