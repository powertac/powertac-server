(function () {
    'use strict';

    angular
        .module('visualizer2App')
        .factory('Push', Push);

    Push.$inject = ['$http', '$cookies', '$q', '$timeout', '$log', 'DEBUG_INFO_ENABLED'];

    var clientId = Math.round((Math.random() * Number.MAX_SAFE_INTEGER)).toString(36);

    function Push ($http, $cookies, $q, $timeout, $log, DEBUG_INFO_ENABLED) {
        var service = {
            receive: receive,
            onConnectionChanged: onConnectionChanged
        };

        var loc = window.location;
        var host = loc.host
        var pathname = loc.pathname
        if (pathname.endsWith('index.html')) {
            pathname = pathname.substring(0, pathname.length - 10);
        }
        var SOCKET_URL = '//' + host + pathname + 'websocket/push';
        var TOPIC = '/topic/push';

        var RECONNECT_TIMEOUT_START = 125;
        var RECONNECT_TIMEOUT_LIMIT = 8000;
        var delay = RECONNECT_TIMEOUT_START;
        var offset = Math.ceil(Math.random() * RECONNECT_TIMEOUT_START);

        var listener = $q.defer();

        var connected = false;
        var connectionChanged = null;

        var socket = {
            client: null,
            stomp: null
        };

        function receive () {
            return listener.promise;
        }

        function onConnectionChanged(callback) {
            connectionChanged = callback;
        }

        function getMessage (data) {
            return JSON.parse(data);
        }

        function success () {
            if (!connected) {
                connected = true;
                connectionChanged(connected);
            }
            // new values
            delay = RECONNECT_TIMEOUT_START;
            socket.stomp.subscribe(TOPIC, function (data) {
                listener.notify(getMessage(data.body));
            }, {
                id: clientId
            });
        }

        function failure (error) {
            if (connected) {
                connected = false;
                connectionChanged(connected);
            }
            $log.debug('STOMP error ' + error);
            $log.debug('reconnecting in ~' + delay + ' ms.');
            $timeout(function () {
                delay = Math.min(2 * delay, RECONNECT_TIMEOUT_LIMIT);
                initialize();
            }, delay + offset);
        }

        function initialize () {
            var headers = {};
            headers[$http.defaults.xsrfHeaderName] = $cookies.get($http.defaults.xsrfCookieName);
            socket.client = new SockJS(SOCKET_URL);
            socket.stomp = Stomp.over(socket.client);
            socket.stomp.connect(headers, success, failure);
            if (!DEBUG_INFO_ENABLED) {
                socket.stomp.debug = null;
            }
        }

        initialize();

        return service;
    }

})();
