(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('game', {
            parent: 'entity',
            url: '/game?page&sort&search',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Games'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/game/games.html',
                    controller: 'GameController',
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
        .state('game-detail', {
            parent: 'game',
            url: '/game/{id}',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'Game'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/game/game-detail.html',
                    controller: 'GameDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entity: ['$stateParams', 'Game', function($stateParams, Game) {
                    return Game.get({id : $stateParams.id}).$promise;
                }],
                previousState: ["$state", function ($state) {
                    var currentStateData = {
                        name: $state.current.name || 'game',
                        params: $state.params,
                        url: $state.href($state.current.name, $state.params)
                    };
                    return currentStateData;
                }]
            }
        })
        .state('game-detail.edit', {
            parent: 'game-detail',
            url: '/detail/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game/game-dialog.html',
                    controller: 'GameDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Game', function(Game) {
                            return Game.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('^', {}, { reload: false });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('game.new', {
            parent: 'game',
            url: '/new',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game/game-dialog.html',
                    controller: 'GameDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: function () {
                            return {
                                type: null,
                                name: null,
                                shared: false,
                                date: null,
                                brokers: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('game', null, { reload: 'game' });
                }, function() {
                    $state.go('game');
                });
            }]
        })
        .state('game.edit', {
            parent: 'game',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game/game-dialog.html',
                    controller: 'GameDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['Game', function(Game) {
                            return Game.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('game', null, { reload: 'game' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('game.delete', {
            parent: 'game',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_USER']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/game/game-delete-dialog.html',
                    controller: 'GameDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Game', function(Game) {
                            return Game.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('game', null, { reload: 'game' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
