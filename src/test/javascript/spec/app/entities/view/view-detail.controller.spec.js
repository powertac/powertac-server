'use strict';

describe('Controller Tests', function() {

    describe('View Management Detail Controller', function() {
        var $scope, $rootScope;
        var MockEntity, MockPreviousState, MockView, MockUser, MockChart;
        var createController;

        beforeEach(inject(function($injector) {
            $rootScope = $injector.get('$rootScope');
            $scope = $rootScope.$new();
            MockEntity = jasmine.createSpy('MockEntity');
            MockPreviousState = jasmine.createSpy('MockPreviousState');
            MockView = jasmine.createSpy('MockView');
            MockUser = jasmine.createSpy('MockUser');
            MockChart = jasmine.createSpy('MockChart');
            

            var locals = {
                '$scope': $scope,
                '$rootScope': $rootScope,
                'entity': MockEntity,
                'previousState': MockPreviousState,
                'View': MockView,
                'User': MockUser,
                'Chart': MockChart
            };
            createController = function() {
                $injector.get('$controller')("ViewDetailController", locals);
            };
        }));


        describe('Root Scope Listening', function() {
            it('Unregisters root scope listener upon scope destruction', function() {
                var eventType = 'visualizer2App:viewUpdate';

                createController();
                expect($rootScope.$$listenerCount[eventType]).toEqual(1);

                $scope.$destroy();
                expect($rootScope.$$listenerCount[eventType]).toBeUndefined();
            });
        });
    });

});
