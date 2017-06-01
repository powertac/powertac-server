(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GraphsController', GraphsController);

    GraphsController.$inject = ['$scope', 'State'];

    function GraphsController ($scope, State) {
        var vm = this;

        vm.state = State;
        vm.changeDetection = {};
        vm.tab = 'retail';

        var chartConfig = {
            chart: {
                animation: false,
                zoomType: 'x'
            },
            rangeSelector: {
                enabled: true
            },
            navigator: {
                enabled: true,
                adaptToUpdatedData: false,
                series: {
                    type: 'line',
                    lineWidth: 0,
                }
            },
            series: [],
            title: {
                text: ''
            },
            chartType: 'stock',
            credits: {
                enabled: false
            }
        };

        function initCharts () {
            Object.keys(vm.state.allGraphKeys).forEach(function(key) {
                vm[key] = angular.copy(chartConfig);
                vm.changeDetection[key] = function(config) {
                    var same = true;
                    if (config.series.length) {
                        same = !State.changed[key];
                        State.changed[key] = false;
                    }
                    return same;
                };
            });

            var colors = ['#7cb5ec', '#434348', '#90ed7d', '#f7a35c', '#8085e9',
                '#f15c80', '#e4d354', '#2b908f', '#f45b5b', '#91e8e1'];
            if (vm.state.competition && vm.state.competition.simulationBaseTime) {
                vm.duration = vm.state.competition.timeslotDuration; // one hour
                vm.start = vm.state.competition.simulationBaseTime.millis;
                // Uggh.. Detect boot game by number of brokers
                if (State.brokers.length > 1) {
                    vm.start += vm.duration * (vm.state.competition.bootstrapTimeslotCount + vm.state.competition.bootstrapDiscardedTimeslots);
                }
            }
            State.brokers.forEach(function (broker, index) {
                var color = colors[index % colors.length];

                Object.keys(vm.state.allGraphKeys).forEach(function(key) {
                    vm[key].series.push(angular.extend(
                        {
                            id: key + '_' + broker.id,
                            name: broker.name,
                            color: color,
                            data: broker.graphData[key],
                            pointStart: vm.start,
                            pointInterval: vm.duration,
                            type: 'line',
                            marker: { enabled: true, radius: 1 },
                            lineWidth: 1,
                            states: {
                                hover: { lineWidth: 1 }
                            }
                        },
                        vm.state.allGraphKeys[key]
                    ));
                });

            });
        }

        $scope.$on('gameInitialized', function (/*event*/) {
            console.log('gameInitialized');
            initCharts();
        });

        initCharts();
    }
})();
