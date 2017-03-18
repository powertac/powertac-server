(function() {
    'use strict';
    angular
        .module('visualizer2App')
        .factory('Graph', Graph);

    Graph.$inject = ['$resource'];

    function Graph ($resource) {
        var resourceUrl =  'api/graphs/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                    }
                    return data;
                }
            },
            'update': { method:'PUT' }
        });
    }
})();
