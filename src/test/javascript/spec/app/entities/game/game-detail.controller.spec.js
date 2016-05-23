'use strict';

describe('Controller Tests', function() {

    describe('Game Management Detail Controller', function() {
        var $scope, $rootScope;
        var MockEntity, MockGame, MockUser, MockFile;
        var createController;

        beforeEach(inject(function($injector) {
            $rootScope = $injector.get('$rootScope');
            $scope = $rootScope.$new();
            MockEntity = jasmine.createSpy('MockEntity');
            MockGame = jasmine.createSpy('MockGame');
            MockUser = jasmine.createSpy('MockUser');
            MockFile = jasmine.createSpy('MockFile');
            

            var locals = {
                '$scope': $scope,
                '$rootScope': $rootScope,
                'entity': MockEntity ,
                'Game': MockGame,
                'User': MockUser,
                'File': MockFile
            };
            createController = function() {
                $injector.get('$controller')("GameDetailController", locals);
            };
        }));


        describe('Root Scope Listening', function() {
            it('Unregisters root scope listener upon scope destruction', function() {
                var eventType = 'visualizer2App:gameUpdate';

                createController();
                expect($rootScope.$$listenerCount[eventType]).toEqual(1);

                $scope.$destroy();
                expect($rootScope.$$listenerCount[eventType]).toBeUndefined();
            });
        });
    });

});
