(function () {

    angular
        .module('visualizer2App')
        .service('State', State);

    State.$inject = ['$rootScope', 'Push'];

    function State ($rootScope, Push) {
        var service = this;

        service.brokers = [];
        service.customers = [];
        service.aggCustomers = [];
        service.timeInstances = [];
        service.gameName = '';
        service.time = '';
        service.timeSlot = '';
        service.timeInstance = '';
        service.queue = [];
        service.gameStatus = '';
        service.prevStatus = '';
        service.gameStatusStyle = 'default';

        service.retailGraphKeys = {
            'retailMoney': {},
            'retailMoneyCumulative': {},
            'retailKwh': {},
            'retailKwhCumulative': {},
            'retailSubscription': {},
            'retailSubscriptionCumulative': {}
        };

        service.wholesaleGraphKeys = {
            'wholesaleMoney': {},
            'wholesaleMoneyCumulative': {},
            'wholesaleMwh': {},
            'wholesaleMwhCumulative': {},
            'wholesalePrice': {},
            'wholesalePriceBuy': {},
            'wholesalePriceSell': {},
        };

        service.allGraphKeys = {
            'allMoneyCumulative': {},
        };
        Object.keys(service.retailGraphKeys).forEach(function(key) {
            service.allGraphKeys[key] = service.retailGraphKeys[key];
        });
        Object.keys(service.wholesaleGraphKeys).forEach(function(key) {
            service.allGraphKeys[key] = service.wholesaleGraphKeys[key];
        });

        service.changed = Object.keys(service.allGraphKeys).reduce(function(map, key) {
            map[key] = false;
            return map;
        }, {});

        Push.receive().then(null, null, function (obj) {
            handlePushMessage(obj);
        });

        Push.onConnectionChanged(setConnected);

        function initGraphData (keys) {
            return Object.keys(keys).reduce(function(map, key) {
                map[key] = [];
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

        function initWholesale (wholesale) {
            wholesale.mwh = 0;
            wholesale.m = 0;
            wholesale.p = NaN;
            wholesale.pb = NaN;
            wholesale.ps = NaN;
            return wholesale;
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
                'graphData': initGraphData(service.retailGraphKeys)
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
            service.timeSlot = snapshot.timeSlot;
            service.timeInstance = new Date(snapshot.timeInstance);
            service.timeInstances.push(service.timeInstance);
            service.timeString = '#' + service.timeSlot + ' | ' + Highcharts.dateFormat('%e %b %Y %H:%M', service.timeInstance) + ' UTC';

            // process broker ticks:
            snapshot.tickValueBrokers.forEach(function (brokerTick) {
                processBrokerTick(brokerTick);
            });

            // prepare customer for a tick:
            service.aggCustomers.forEach(function (customer) {
                prepareCustomer(customer);
            });

            // process customer ticks:
            missing = Object.keys(service.customers).reduce(function(missing, id) {
                missing[id] = true;
                return missing;
            }, {});
            snapshot.tickValueCustomers.forEach(function (customerTick) {
                missing[customerTick.id] = false;
                processCustomerTick(customerTick);
            });
            Object.keys(missing).forEach(function(id) {
                if (missing[id]) {
                    processCustomerTick({
                        id: id
                    });
                }
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
                broker.wholesale = initWholesale(broker.wholesale);

                // add some arrays for graphs:
                broker.graphData = initGraphData(service.allGraphKeys);

                // begin with every broker enabled / checked
                broker.enabled = true;

                // add to service.brokers collection:
                service.brokers[broker.id] = broker;
            });
        }

        function processBrokerTick (brokerTick) {
            var broker = service.brokers[brokerTick.id];
            var retail = brokerTick.retail;
            var wholesale = brokerTick.wholesale;

            var cash = brokerTick.hasOwnProperty('cash') ? brokerTick.cash : 0;
            broker.cash = cash;
            broker.graphData.allMoneyCumulative.push(cash);

            var sub = retail && retail.hasOwnProperty('sub') ? retail.sub : 0;
            broker.retail.sub += sub;
            broker.graphData.retailSubscription.push(sub);
            broker.graphData.retailSubscriptionCumulative.push(broker.retail.sub);

            var rkwh = retail && retail.hasOwnProperty('kwh') ? retail.kwh : 0;
            broker.retail.kwh += rkwh;
            broker.graphData.retailKwh.push(rkwh);
            broker.graphData.retailKwhCumulative.push(broker.retail.kwh);

            var rm = retail && retail.hasOwnProperty('m') ? retail.m : 0;
            broker.retail.m += rm;
            broker.graphData.retailMoney.push(rm);
            broker.graphData.retailMoneyCumulative.push(broker.retail.m);

            var wmwh = wholesale && wholesale.hasOwnProperty('mwh') ? wholesale.mwh : 0;
            broker.wholesale.mwh += wmwh;
            broker.graphData.wholesaleMwh.push(wmwh);
            broker.graphData.wholesaleMwhCumulative.push(broker.wholesale.mwh);

            var wm = wholesale && wholesale.hasOwnProperty('m') ? wholesale.m : 0;
            broker.wholesale.m += wm;
            broker.graphData.wholesaleMoney.push(wm);
            broker.graphData.wholesaleMoneyCumulative.push(broker.wholesale.m);

            var p = wholesale && wholesale.hasOwnProperty('p') ? wholesale.p : NaN;
            broker.wholesale.p = isNaN(p) ? null : p;
            broker.graphData.wholesalePrice.push(broker.wholesale.p);

            var pb = wholesale && wholesale.hasOwnProperty('pb') ? wholesale.pb : NaN;
            broker.wholesale.pb = isNaN(pb) ? null : pb;
            broker.graphData.wholesalePriceBuy.push(broker.wholesale.pb);

            var ps = wholesale && wholesale.hasOwnProperty('ps') ? wholesale.ps : NaN;
            broker.wholesale.ps = isNaN(ps) ? null : ps;
            broker.graphData.wholesalePriceSell.push(broker.wholesale.ps);

            if (retail && retail.hasOwnProperty('actTx')) {
                broker.retail.actTx += retail.actTx;
            }
            if (retail && retail.hasOwnProperty('rvkTx')) {
                broker.retail.rvkTx += retail.rvkTx;
            }
            if (retail && retail.hasOwnProperty('pubTx')) {
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

                customer.retail = initRetail(customer.retail);
                service.customers[customer.id] = customer;
            });
        }

        function prepareCustomer (customer) {
            var lastIndex = customer.graphData.retailSubscription.length - 1;
            customer.graphData.retailSubscription.push(0);
            customer.graphData.retailSubscriptionCumulative.push(
                customer.graphData.retailSubscriptionCumulative[lastIndex]);

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
            var lastIndex = aggCustomer.graphData.retailSubscription.length - 1;

            if (retail && retail.hasOwnProperty('sub')) {
                aggCustomer.retail.sub += retail.sub;
                aggCustomer.graphData.retailSubscription[lastIndex] += retail.sub;
                aggCustomer.graphData.retailSubscriptionCumulative[lastIndex] += retail.sub;
            }

            if (retail && retail.hasOwnProperty('kwh')) {
                aggCustomer.retail.kwh += retail.kwh;
                aggCustomer.graphData.retailKwh[lastIndex] += retail.kwh;
                aggCustomer.graphData.retailKwhCumulative[lastIndex] += retail.kwh;
            }

            if (retail && retail.hasOwnProperty('m')) {
                aggCustomer.retail.m += retail.m;
                aggCustomer.graphData.retailMoney[lastIndex] += retail.m;
                aggCustomer.graphData.retailMoneyCumulative[lastIndex] += retail.m;
            }

            if (retail && retail.hasOwnProperty('actTx')) {
                aggCustomer.retail.actTx += retail.actTx;
            }

            if (retail && retail.hasOwnProperty('rvkTx')) {
                aggCustomer.retail.rvkTx += retail.rvkTx;
            }
            if (retail && retail.hasOwnProperty('pubTx')) {
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
