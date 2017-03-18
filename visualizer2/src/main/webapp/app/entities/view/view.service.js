(function() {
    'use strict';
    angular
        .module('visualizer2App')
        .factory('View', View);

    View.$inject = ['$resource'];

    function View ($resource) {
        var resourceUrl =  'api/views/:id';

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
