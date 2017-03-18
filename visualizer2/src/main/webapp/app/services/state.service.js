(function () {

    angular
        .module('visualizer2App')
        .service('State', State);

    State.$inject = ['$rootScope', 'Push'];

    function State ($rootScope, Push) {
        var service = this;

        service.brokers = [];
        service.aggCustomers = [];
        service.timeInstances = [];
        service.gameName = '';
        service.queue = [];
        service.gameStatus = '';
        service.prevStatus = '';
        service.gameStatusStyle = 'default';

        service.graphKeys = [
            'allMoneyCumulative',
            'retailMoneyCumulative',
            'retailMoney',
            'retailKwhCumulative',
            'retailKwh',
            'subscription',
            'subscriptionCumulative'
        ];

        service.changed = Object.keys(initGraphData()).reduce(function(map, key) {
            map[key] = false;
            return map;
        }, {});

        Push.receive().then(null, null, function (obj) {
            handlePushMessage(obj);
        });

        Push.onConnectionChanged(setConnected);

        function initGraphData () {
            return service.graphKeys.reduce(function(map, key) {
                map[key] = [0];
                return map;
            }, {});
        }

        function initRetail (retail) {
            retail.sub = 0;
            retail.kwh = 0;
            retail.m = 0;
            retail.actTx = 0;
            retail.rvkTx = 0;
            retail.pubTx = 0;
            return retail;
        }

        function aggCustomer (powerType) {
            return {
                'genericPowerType': '',
                'powerType': powerType,
                // here we will keep id for each concrete customer so we can
                // find the powerTypeCustomer
                'ids': [],
                'customerClass': [],
                'population': 0,
                'retail': initRetail({}),
                'graphData': initGraphData()
            };
        }

        function setConnected(connected) {
            if (connected) {
                setStatus(service.prevStatus);
            } else {
                service.prevStatus = service.gameStatus;
                setStatus('OFFLINE');
            }
        }

        function setStatus (status) {
            service.gameStatus = status;

            if (status === 'RUNNING') {
                service.gameStatusStyle = 'success';
            } else if (status === 'WAITING') {
                service.gameStatusStyle = 'info';
            } else if (status === 'FINISHED') {
                service.gameStatusStyle = 'warning';
            } else if (status === 'IDLE') {
                service.gameStatusStyle = 'default';
            } else {
                service.gameStatusStyle = 'danger'; // unexpected
            }
        }

        function processCompetition (competition) {
            service.competition = competition;
        }

        function processSnapshot (snapshot) {
            service.timeInstances.push(new Date(snapshot.timeInstance));

            // process broker ticks:
            snapshot.tickValueBrokers.forEach(function (brokerTick) {
                processBrokerTick(brokerTick);
            });

            // prepare customer for a tick:
            service.aggCustomers.forEach(function (customer) {
                prepareCustomer(customer);
            });

            // process customer ticks:
            snapshot.tickValueCustomers.forEach(function (customerTick) {
                processCustomerTick(customerTick);
            });

            // mark as dirty
            Object.keys(service.changed).forEach(function(key) {
                service.changed[key] = true;
            });
        };

        function processBrokers (brokers) {
            service.brokers = [];

            brokers.forEach(function (broker) {
                // make a properties if they do not exist (back-end will not
                // send a property unless it has a value.)
                broker.cash = 0;
                broker.retail = initRetail(broker.retail);

                // add some arrays for graphs:
                broker.graphData = initGraphData();

                // add to service.brokers collection:
                service.brokers[broker.id] = broker;
            });
        }

        function processBrokerTick (brokerTick) {
            var retail = brokerTick.retail;
            var broker = service.brokers[brokerTick.id];

            var cash = brokerTick.hasOwnProperty('cash') ? brokerTick.cash : 0;
            broker.cash = cash;
            broker.graphData.allMoneyCumulative.push(cash);

            var sub = retail.hasOwnProperty('sub') ? retail.sub : 0;
            broker.retail.sub += sub;
            broker.graphData.subscription.push(sub);
            broker.graphData.subscriptionCumulative.push(broker.retail.sub);

            var kwh = retail.hasOwnProperty('kwh') ? retail.kwh : 0;
            broker.retail.kwh += kwh;
            broker.graphData.retailKwh.push(kwh);
            broker.graphData.retailKwhCumulative.push(broker.retail.kwh);

            var m = retail.hasOwnProperty('m') ? retail.m : 0;
            broker.retail.m += m;
            broker.graphData.retailMoney.push(m);
            broker.graphData.retailMoneyCumulative.push(broker.retail.m);

            if (retail.hasOwnProperty('actTx')) {
                broker.retail.actTx += retail.actTx;
            }
            if (retail.hasOwnProperty('rvkTx')) {
                broker.retail.rvkTx += retail.rvkTx;
            }
            if (retail.hasOwnProperty('pubTx')) {
                broker.retail.pubTx += retail.pubTx;
            }
        }

        function processCustomers (customers) {
            // These are tightly coupled
            service.aggCustomers = [];
            service.powerTypes = [];
            // Map customer.id to powerType (not available in the tick)
            service.powerTypeMap = {};

            // TODO optimize data structures
            customers.forEach(function (customer) {
                var powerType = customer.powerType;
                var powerIndex = service.powerTypes.indexOf(powerType);
                if (powerIndex === -1) {
                    service.powerTypes.push(powerType);
                    service.aggCustomers.push(aggCustomer(powerType));
                    powerIndex = service.powerTypes.length - 1;
                }

                service.powerTypeMap[customer.id] = powerIndex;

                // TODO This isn't right, why are generic and class overwritten?
                // customerClass not used yet
                service.aggCustomers[powerIndex].genericPowerType = customer.genericPowerType;
                service.aggCustomers[powerIndex].ids.push(customer.id);
                service.aggCustomers[powerIndex].customerClass = customer.customerClass;
                service.aggCustomers[powerIndex].population += customer.population;
            });
        }

        function prepareCustomer (customer) {
            var lastIndex = customer.graphData.subscription.length - 1;
            customer.graphData.subscription.push(0);
            customer.graphData.subscriptionCumulative.push(
                customer.graphData.subscriptionCumulative[lastIndex]);

            customer.graphData.retailKwh.push(0);
            customer.graphData.retailKwhCumulative.push(
                customer.graphData.retailKwhCumulative[lastIndex]);

            customer.graphData.retailMoney.push(0);
            customer.graphData.retailMoneyCumulative.push(
                customer.graphData.retailMoneyCumulative[lastIndex]);
        }

        function processCustomerTick (customerTick) {
            var retail = customerTick.retail;
            var powerIndex = service.powerTypeMap[customerTick.id];
            var aggCustomer = service.aggCustomers[powerIndex];
            var lastIndex = aggCustomer.graphData.subscription.length - 1;

            if (retail.hasOwnProperty('sub')) {
                aggCustomer.retail.sub += retail.sub;
                aggCustomer.graphData.subscription[lastIndex] += retail.sub;
                aggCustomer.graphData.subscriptionCumulative[lastIndex] += retail.sub;
            }

            if (retail.hasOwnProperty('kwh')) {
                aggCustomer.retail.kwh += retail.kwh;
                aggCustomer.graphData.retailKwh[lastIndex] += retail.kwh;
                aggCustomer.graphData.retailKwhCumulative[lastIndex] += retail.kwh;
            }

            if (retail.hasOwnProperty('m')) {
                aggCustomer.retail.m += retail.m;
                aggCustomer.graphData.retailMoney[lastIndex] += retail.m;
                aggCustomer.graphData.retailMoneyCumulative[lastIndex] += retail.m;
            }

            if (retail.hasOwnProperty('actTx')) {
                aggCustomer.retail.actTx += retail.actTx;
            }

            if (retail.hasOwnProperty('rvkTx')) {
                aggCustomer.retail.rvkTx += retail.rvkTx;
            }
            if (retail.hasOwnProperty('pubTx')) {
                aggCustomer.retail.pubTx += retail.pubTx;
            }
        }

        function handlePushMessage (obj) {
            var message = obj.message;
            var type = obj.type;

            if (type.localeCompare('INIT') === 0) {
                // initialize the front-end model
                service.gameName = obj.game;
                setStatus(message.state);
                service.timeInstances = [];
                processCompetition(message.competition);
                processBrokers(message.brokers);
                processCustomers(message.customers);
                message.snapshots.forEach(function (snapshot) {
                    processSnapshot(snapshot);
                });
                $rootScope.$broadcast('gameInitialized');
                service.queue.forEach(function(obj) {
                    if (obj.game === service.gameName) {
                        handlePushMessage(obj);
                    } else {
                        console.log('ignore ' + obj.type + ' ' + obj.game);
                    }
                });
                service.queue = [];
                return;
            }
            if (service.gameName !== obj.game) {
                console.log('queue ' + obj.type + ' ' + obj.game);
                service.queue.push(obj);
                return;
            }
            if (type.localeCompare('INFO') === 0) {
                setStatus(message);
            }
            else if (type.localeCompare('DATA') === 0) {
                processSnapshot(message);
            }
        }

        return service;
    }

})();
