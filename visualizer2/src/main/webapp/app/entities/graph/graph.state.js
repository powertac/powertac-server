(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('graph', {
            parent: 'entity',
            url: '/graph?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Graphs'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/graph/graphs.html',
                    controller: 'GraphController',
                    controllerAs: 'vm'
                }
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'id,asc',
                    squash: true
                },
                search: null
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtil', function ($stateParams, PaginationUtil) {
                    return {
                        page: PaginationUtil.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtil.parsePredicate($stateParams.sort),
                        ascending: PaginationUtil.parseAscending($stateParams.sort),
                        search: $stateParams.search
                    };
                }],
            }
        })
        .state('graph-detail', {
            parent: 'graph',
            url: '/graph/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Graph'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/graph/graph-detail.html',
                    controller: 'GraphDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Graph', function($stateParams, Graph) {
                    return Graph.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'graph',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('graph-detail.edit', {
            parent: 'graph-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/graph/graph-dialog.html',
                    controller: 'GraphDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Graph', function(Graph) {
                            return Graph.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('graph.new', {
            parent: 'graph',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/graph/graph-dialog.html',
                    controller: 'GraphDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                type: null,
                                shared: false,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('graph', null, { reload: 'graph' });
                }, function() {
                    $state.go('graph');
                });
            }]
        })
        .state('graph.edit', {
            parent: 'graph',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/graph/graph-dialog.html',
                    controller: 'GraphDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Graph', function(Graph) {
                            return Graph.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('graph', null, { reload: 'graph' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('graph.delete', {
            parent: 'graph',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/graph/graph-delete-dialog.html',
                    controller: 'GraphDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Graph', function(Graph) {
                            return Graph.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('graph', null, { reload: 'graph' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
