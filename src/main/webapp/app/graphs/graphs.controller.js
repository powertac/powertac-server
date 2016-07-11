(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('GraphsController', GraphsController);

    GraphsController.$inject = ['$scope', 'State'];

    function GraphsController ($scope, State) {
        var vm = this;

        vm.state = State;

        var chartConfig = {
            options: {
                chart: {
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
                }
            },
            series: [],
            title: {
                text: ''
            },
            xAxis: {
                type: 'datetime',
                categories: State.timeInstances
            },
            useHighStocks: true
        };

        function initCharts () {
            vm.allMoneyCumulativesConfig = angular.copy(chartConfig);
            vm.retailMoneyCumulativesConfig = angular.copy(chartConfig);
            vm.retailMoneysConfig = angular.copy(chartConfig);
            vm.retailKwhCumulativesConfig = angular.copy(chartConfig);
            vm.retailKwhsConfig = angular.copy(chartConfig);
            vm.subscriptionsConfig = angular.copy(chartConfig);
            vm.subscriptionCumulativesConfig = angular.copy(chartConfig);

            State.brokers.forEach(function (broker) {
                vm.allMoneyCumulativesConfig.series.push({
                    name: broker.name,
                    data: broker.graphData.allMoneyCumulative,
                    pointStart: vm.state.competition.simulationBaseTime.millis,
                    pointInterval: vm.state.competition.timeslotDuration // one day
                });
                vm.retailMoneyCumulativesConfig.series.push({
                    name: broker.name,
                    data: broker.graphData.retailMoneyCumulative,
                    pointStart: vm.state.competition.simulationBaseTime.millis,
                    pointInterval: vm.state.competition.timeslotDuration // one day
                });
                vm.retailMoneysConfig.series.push({
                    name: broker.name,
                    data: broker.graphData.retailMoney,
                    pointStart: vm.state.competition.simulationBaseTime.millis,
                    pointInterval: vm.state.competition.timeslotDuration // one day
                });
                vm.retailKwhCumulativesConfig.series.push({
                    name: broker.name,
                    data: broker.graphData.retailKwhCumulative,
                    pointStart: vm.state.competition.simulationBaseTime.millis,
                    pointInterval: vm.state.competition.timeslotDuration // one day
                });
                vm.retailKwhsConfig.series.push({
                    name: broker.name,
                    data: broker.graphData.retailKwh,
                    pointStart: vm.state.competition.simulationBaseTime.millis,
                    pointInterval: vm.state.competition.timeslotDuration // one day
                });
                vm.subscriptionsConfig.series.push({
                    name: broker.name,
                    data: broker.graphData.subscription,
                    pointStart: vm.state.competition.simulationBaseTime.millis,
                    pointInterval: vm.state.competition.timeslotDuration // one day
                });
                vm.subscriptionCumulativesConfig.series.push({
                    name: broker.name,
                    data: broker.graphData.subscriptionCumulative,
                    pointStart: vm.state.competition.simulationBaseTime.millis,
                    pointInterval: vm.state.competition.timeslotDuration // one day
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
