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
        vm.graphs = {};

        vm.colors = [ '#7cb5ec', '#434348', '#90ed7d', '#f7a35c', '#8085e9',
            '#f15c80', '#e4d354', '#2b908f', '#91e8e1','#f45b5b'];

        vm.symbolMap = {
            'circle': '&#9679',
            'square': '&#9632',
            'diamond': '&#9670',
            'triangle': '&#9650',
            'triangle-down': '&#9660'
        };
        vm.symbols = Object.keys(vm.symbolMap);

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
                },
                height: 16
            },
            series: [],
            title: {
                text: ''
            },
            tooltip: {
                useHTML: true,
                shared: true,
                formatter: function() {
                    var head = '<small>' + Highcharts.dateFormat('%e %b %Y &nbsp; %H:%M', this.x) + '</small>' +
                        '<table style="min-width: 175px"><tr><td colspan="3">&nbsp;</td></tr>';
                    var rows = [];
                    this.points.forEach(function(point) {
                        var symbol = vm.symbolMap[point.series.symbol];
                        var value = point.y.toFixed(2).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");

                        rows.push([point.y,
                            '<tr>' +
                            '<td style="text-align: center; color: ' + point.series.color + '">' + symbol + '</td>' +
                            '<td style="text-align: left">' + point.series.name + '</td>' +
                            '<td style="text-align: right"><b>' + value + '</b></td>' +
                            '</tr>']);
                    });

                    var foot = '</table>';

                    // Sort rows
                    rows = rows.sort(function(a, b) {
                        return b[0] - a[0];
                    }).map(function(a) {
                        return a[1];
                    });

                    return head + rows.join('') + foot;
                }

            },
            plotOptions: { series: { states: { hover: { lineWidth: 2 } } } },
            chartType: 'stock',
            credits: {
                enabled: false
            }
        };

        vm.toggleBroker = function (index) {
            var broker = State.brokers[index];
            var enabled = !broker.enabled;
            broker.enabled = enabled;
            Object.keys(vm.graphs).forEach(function(key) {
                vm.graphs[key].series.some(function(series, index) {
                    if (series.id === key + '_' + broker.id) {
                        series.visible = broker.enabled;
                        return true;
                    }
                });
                State.changed[key] = true;
            });
        };

        function initCharts () {
            Object.keys(vm.state.allGraphKeys).forEach(function(key) {
                vm.graphs[key] = angular.copy(chartConfig);
                vm.changeDetection[key] = function(config) {
                    if (!key.startsWith(vm.tab) && !key.startsWith('all')) {
                        return true;
                    }
                    var same = true;
                    if (config.series.length) {
                        same = !State.changed[key];
                        State.changed[key] = false;
                    }
                    return same;
                };
            });

            if (vm.state.competition && vm.state.competition.simulationBaseTime) {
                vm.duration = vm.state.competition.timeslotDuration; // one hour
                vm.start = Date.parse(vm.state.competition.simulationBaseTime);
                // Uggh.. Detect boot game by number of brokers
                if (State.brokers.length > 1) {
                    vm.start += vm.duration * (vm.state.competition.bootstrapTimeslotCount + vm.state.competition.bootstrapDiscardedTimeslots);
                }
            }
            State.brokers.forEach(function (broker, index) {
                var color = vm.colors[index % vm.colors.length];
                var symbol = vm.symbols[index % vm.symbols.length];

                Object.keys(vm.state.allGraphKeys).forEach(function(key) {
                    vm.graphs[key].series.push(angular.extend(
                        {
                            id: key + '_' + broker.id,
                            name: broker.name,
                            color: color,
                            marker: {
                                enabled: true,
                                radius: 1,
                                symbol: symbol
                            },
                            visible: broker.enabled,
                            data: broker.graphData[key],
                            pointStart: vm.start,
                            pointInterval: vm.duration,
                            type: 'line',
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
