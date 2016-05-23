(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GameDeleteController',GameDeleteController);

    GameDeleteController.$inject = ['$uibModalInstance', 'entity', 'Game'];

    function GameDeleteController($uibModalInstance, entity, Game) {
        var vm = this;
        vm.game = entity;
        vm.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
        vm.confirmDelete = function (id) {
            Game.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        };
    }
})();
