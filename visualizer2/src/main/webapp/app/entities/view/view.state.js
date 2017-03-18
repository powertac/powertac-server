(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('view', {
            parent: 'entity',
            url: '/view?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Views'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/view/views.html',
                    controller: 'ViewController',
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
        .state('view-detail', {
            parent: 'view',
            url: '/view/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'View'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/view/view-detail.html',
                    controller: 'ViewDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'View', function($stateParams, View) {
                    return View.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'view',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('view-detail.edit', {
            parent: 'view-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/view/view-dialog.html',
                    controller: 'ViewDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['View', function(View) {
                            return View.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('view.new', {
            parent: 'view',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/view/view-dialog.html',
                    controller: 'ViewDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                graphs: null,
                                shared: false,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('view', null, { reload: 'view' });
                }, function() {
                    $state.go('view');
                });
            }]
        })
        .state('view.edit', {
            parent: 'view',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/view/view-dialog.html',
                    controller: 'ViewDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['View', function(View) {
                            return View.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('view', null, { reload: 'view' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('view.delete', {
            parent: 'view',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/view/view-delete-dialog.html',
                    controller: 'ViewDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['View', function(View) {
                            return View.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('view', null, { reload: 'view' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
