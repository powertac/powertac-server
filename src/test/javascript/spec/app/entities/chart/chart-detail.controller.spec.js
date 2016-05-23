'use strict';

describe('Controller Tests', function() {

    describe('Chart Management Detail Controller', function() {
        var $scope, $rootScope;
        var MockEntity, MockChart, MockUser, MockGraph;
        var createController;

        beforeEach(inject(function($injector) {
            $rootScope = $injector.get('$rootScope');
            $scope = $rootScope.$new();
            MockEntity = jasmine.createSpy('MockEntity');
            MockChart = jasmine.createSpy('MockChart');
            MockUser = jasmine.createSpy('MockUser');
            MockGraph = jasmine.createSpy('MockGraph');
            

            var locals = {
                '$scope': $scope,
                '$rootScope': $rootScope,
                'entity': MockEntity ,
                'Chart': MockChart,
                'User': MockUser,
                'Graph': MockGraph
            };
            createController = function() {
                $injector.get('$controller')("ChartDetailController", locals);
            };
        }));


        describe('Root Scope Listening', function() {
            it('Unregisters root scope listener upon scope destruction', function() {
                var eventType = 'visualizer2App:chartUpdate';

                createController();
                expect($rootScope.$$listenerCount[eventType]).toEqual(1);

                $scope.$destroy();
                expect($rootScope.$$listenerCount[eventType]).toBeUndefined();
            });
        });
    });

});
