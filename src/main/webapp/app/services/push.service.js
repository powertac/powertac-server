(function () {
    'use strict';

    angular
        .module('visualizer2App')
        .factory('Push', Push);

    Push.$inject = ['$q', '$timeout', '$log'];

    function Push ($q, $timeout, $log) {
        var service = {
            receive: receive
        };

        var loc = window.location;
        var SOCKET_URL = '//' + loc.host + loc.pathname + 'websocket/push';
        var TOPIC = '/topic/push';
        var RECONNECT_TIMEOUT = 30000;

        var listener = $q.defer();
        var socket = {
            client: null,
            stomp: null
        };

        function receive () {
            return listener.promise;
        }

        function reconnect () {
            $log.info('reconnect');
            $timeout(function () {
                initialize();
            }, RECONNECT_TIMEOUT);
        }

        function getMessage (data) {
            return JSON.parse(data);
        }

        function startListener () {
            // new values
            socket.stomp.subscribe(TOPIC, function (data) {
                listener.notify(getMessage(data.body));
            });
        }

        function initialize () {
            socket.client = new SockJS(SOCKET_URL);
            socket.stomp = Stomp.over(socket.client);
            socket.stomp.connect({}, startListener);
            socket.stomp.onclose = reconnect;
        }

        initialize();

        return service;
    }

})();
