(function() {
    'use strict';
    angular
        .module('visualizer2App')
        .factory('Chart', Chart);

    Chart.$inject = ['$resource'];

    function Chart ($resource) {
        var resourceUrl =  'api/charts/:id';

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
