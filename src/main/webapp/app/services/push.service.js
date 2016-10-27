(function () {
    'use strict';

    angular
        .module('visualizer2App')
        .factory('Push', Push);

    Push.$inject = ['$q', '$timeout', '$log', 'DEBUG_INFO_ENABLED'];

    function Push ($q, $timeout, $log, DEBUG_INFO_ENABLED) {
        var service = {
            receive: receive
        };

        var loc = window.location;
        var SOCKET_URL = '//' + loc.host + loc.pathname + 'websocket/push';
        var TOPIC = '/topic/push';

        var RECONNECT_TIMEOUT_START = 500;
        var RECONNECT_TIMEOUT_LIMIT = 32000;
        var delay = RECONNECT_TIMEOUT_START;
        var offset = Math.ceil(Math.random() * RECONNECT_TIMEOUT_START)

        var listener = $q.defer();
        var socket = {
            client: null,
            stomp: null
        };

        function receive () {
            return listener.promise;
        }

        function getMessage (data) {
            return JSON.parse(data);
        }

        function success () {
            // new values
            delay = RECONNECT_TIMEOUT_START
            socket.stomp.subscribe(TOPIC, function (data) {
                listener.notify(getMessage(data.body));
            });
        }

        function failure (error) {
            $log.debug('STOMP error ' + error);
            $log.debug('reconnecting in ~' + delay + ' ms.');
            $timeout(function () {
                delay = Math.min(2 * delay, RECONNECT_TIMEOUT_LIMIT)
                initialize();
            }, delay + offset);
        }

        function initialize () {
            socket.client = new SockJS(SOCKET_URL);
            socket.stomp = Stomp.over(socket.client);
            socket.stomp.connect({}, success, failure);
            if (!DEBUG_INFO_ENABLED) {
                socket.stomp.debug = null;
            }
        }

        initialize();

        return service;
    }

})();
