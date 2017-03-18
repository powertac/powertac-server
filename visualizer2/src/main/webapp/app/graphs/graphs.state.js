(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('graphs', {
            parent: 'app',
            url: '/',
            data: {
                pageTitle: 'Graphs'
            },
            views: {
                'content@': {
                    templateUrl: 'app/graphs/graphs.html',
                    controller: 'GraphsController',
                    controllerAs: 'vm'
                }
            }
        });
    }

})();
