(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig ($stateProvider) {
        $stateProvider.state('files', {
            parent: 'app',
            url: '/files',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Files'
            },
            views: {
                'content@': {
                    templateUrl: 'app/files/files.html',
                    controller: 'FilesController',
                    controllerAs: 'vm'
                }
            }
        });
    }

})();
