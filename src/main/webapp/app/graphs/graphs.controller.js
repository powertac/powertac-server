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
            vm.allMoneyCumulativesConfig = angular.copy(chartConfig);
            vm.retailMoneyCumulativesConfig = angular.copy(chartConfig);
            vm.retailMoneysConfig = angular.copy(chartConfig);
            vm.retailKwhCumulativesConfig = angular.copy(chartConfig);
            vm.retailKwhsConfig = angular.copy(chartConfig);
            vm.subscriptionsConfig = angular.copy(chartConfig);
            vm.subscriptionCumulativesConfig = angular.copy(chartConfig);

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


                vm.allMoneyCumulativesConfig.series.push({
                    name: broker.name,
                    color: color,
                    data: broker.graphData.allMoneyCumulative,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailMoneyCumulativesConfig.series.push({
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailMoneyCumulative,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailMoneysConfig.series.push({
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailMoney,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailKwhCumulativesConfig.series.push({
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailKwhCumulative,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.retailKwhsConfig.series.push({
                    name: broker.name,
                    color: color,
                    data: broker.graphData.retailKwh,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.subscriptionsConfig.series.push({
                    name: broker.name,
                    color: color,
                    data: broker.graphData.subscription,
                    pointStart: vm.start,
                    pointInterval: vm.duration
                });
                vm.subscriptionCumulativesConfig.series.push({
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
