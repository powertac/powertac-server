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
                    var head = '<small>' + Highcharts.dateFormat('%e %b %Y &nbsp; %H:%M', new Date(this.x)) + '</small>' +
                        '<table style="min-width: 175px"><tr><td colspan="3">&nbsp;</td></tr>';
                    var rows = [];
                    this.points.forEach(function(point) {
                        var symbol;

                        switch (point.series.symbol) {
                            case 'diamond':
                                symbol = '&#9670';
                                break;
                            case 'square':
                                symbol = '&#9632';
                                break;
                            case 'triangle':
                                symbol = '&#9650';
                                break;
                            case 'triangle-down':
                                symbol = '&#9660';
                                break;
                            case 'circle':
                            default:
                                symbol = '&#9679';
                                break;
                        }

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

        function initCharts () {
            Object.keys(vm.state.allGraphKeys).forEach(function(key) {
                vm[key] = angular.copy(chartConfig);
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
