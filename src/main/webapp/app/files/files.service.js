(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .factory('Files', Files);

    Files.$inject = ['$resource'];

    function Files ($resource) {
        var service = $resource('api/myfiles/:type\\/', {}, {
            'query': {
                method: 'GET',
                params: { type: '@type' },
                isArray: true
            },
            'upload': {
                method: 'POST',
                params: { type: '@type' },
                transformRequest: function (data) {
                    if (data === undefined) {
                      return data;
                    }
                    var fd = new FormData();
                    angular.forEach(data, function (value, key) {
                        if (value instanceof FileList) {
                            if (value.length === 1) {
                                fd.append(key, value[0]);
                            } else {
                                angular.forEach(value, function (file, index) {
                                    fd.append(key + '_' + index, file);
                                });
                            }
                        } else {
                            fd.append(key, value);
                        }
                    });
                    return fd;
                },
                headers: {
                    'Content-Type': undefined
                }
            }
        })

        return service;
    }
})();
