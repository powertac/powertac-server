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
            vm.state.graphKeys.forEach(function(key) {
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

                vm.allMoneyCumulative.series.push({
                    id: 'allMoneyCumulative_' + broker.id,
                    name: broker.name,
                    color: color,
                    data: broker.graphData.allMoneyCumulative,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailMoneyCumulative.series.push({
                    id: 'retailMoneyCumulative_' + broker.id,
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailMoneyCumulative,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailMoney.series.push({
                    id: 'retailMoney_' + broker.id,
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailMoney,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailKwhCumulative.series.push({
                    id: 'retailKwhCumulative_' + broker.id,
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailKwhCumulative,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailKwh.series.push({
                    id: 'retailKwh_' + broker.id,
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailKwh,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.subscription.series.push({
                    id: 'subscription_' + broker.id,
                    name: broker.name,
                    color: color,
                    data: broker.graphData.subscription,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.subscriptionCumulative.series.push({
                    id: 'subscriptionCumulative_' + broker.id,
                    name: broker.name,
                    color: color,
                    data: broker.graphData.subscriptionCumulative,
                    pointStart: vm.start,
                    pointInterval: vm.duration
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
