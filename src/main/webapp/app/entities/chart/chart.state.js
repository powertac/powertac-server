(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('chart', {
            parent: 'entity',
            url: '/chart?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Charts'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/chart/charts.html',
                    controller: 'ChartController',
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
        .state('chart-detail', {
            parent: 'chart',
            url: '/chart/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Chart'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/chart/chart-detail.html',
                    controller: 'ChartDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Chart', function($stateParams, Chart) {
                    return Chart.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'chart',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('chart-detail.edit', {
            parent: 'chart-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/chart/chart-dialog.html',
                    controller: 'ChartDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Chart', function(Chart) {
                            return Chart.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('chart.new', {
            parent: 'chart',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/chart/chart-dialog.html',
                    controller: 'ChartDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                shared: false,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('chart', null, { reload: 'chart' });
                }, function() {
                    $state.go('chart');
                });
            }]
        })
        .state('chart.edit', {
            parent: 'chart',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/chart/chart-dialog.html',
                    controller: 'ChartDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Chart', function(Chart) {
                            return Chart.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('chart', null, { reload: 'chart' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('chart.delete', {
            parent: 'chart',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/chart/chart-delete-dialog.html',
                    controller: 'ChartDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Chart', function(Chart) {
                            return Chart.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('chart', null, { reload: 'chart' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
