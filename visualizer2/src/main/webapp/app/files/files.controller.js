(function() {
    'use strict';

    angular
        .module('visualizer2App')
        .controller('FilesController', FilesController);

    FilesController.$inject = ['$window', 'AlertService', 'File', 'Files'];

    function FilesController ($window, AlertService, File, Files) {
        var vm = this;
        vm.refreshFiles = refreshFiles; // TODO push changes via websockets?
        vm.resetUpload = resetUpload;
        vm.doUpload = doUpload;
        vm.doDownload = doDownload;
        vm.doDelete = doDelete;

        vm.itemsPerPage = 10;

        vm.refreshFiles();
        vm.resetUpload();

        function refreshFiles () {
            vm.filesCollection = Files.query({type:'any'});
        }

        function resetUpload () {
            vm.uploadFile = null;
            vm.uploadShared = false;
            vm.uploadOverwrite = false;
            vm.uploadType = '';
        }

        function doUpload () {
            Files.upload({
                file: vm.uploadFile,
                type: vm.uploadType,
                shared: vm.uploadShared,
                overwrite: vm.uploadOverwrite
            }, function() {
                angular.element('#fileForm')[0].reset();
                vm.resetUpload();
                vm.refreshFiles();
            }, function(err) {
                AlertService.error('Error: ' + err.data.message + ' (' + err.status + ' ' + err.statusText + ')');
            });
        }

        function doDelete (id) {
            File.delete({id: id}, function() {
                vm.refreshFiles();
            });
        }

        function doDownload (type, id) {
            $window.location = 'api/myfiles/' + type.toLowerCase() + '/' + id;
        }
    }

})();
