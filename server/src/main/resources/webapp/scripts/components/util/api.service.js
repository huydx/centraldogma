'use strict';

angular.module('CentralDogmaAdmin')
    .factory('ApiService', function ($rootScope, $http, $q, $window, StringUtil, NotificationUtil, CentralDogmaConstant) {
               function makeRequest(verb, uri, config, data) {
                 var sessionId;
                 var defer = $q.defer();

                 if (angular.isUndefined(config)) {
                   config = {}
                 }

                 config.method = verb;
                 config.url = rewriteUri(uri);

                 if ($rootScope.isSecurityEnabled) {
                   sessionId = $window.sessionStorage.getItem('sessionId');
                 } else {
                   sessionId = "anonymous";
                 }
                 if (sessionId !== null) {
                   if (angular.isUndefined(config.headers)) {
                     config.headers = {};
                   }
                   config.headers.authorization = 'bearer ' + sessionId;
                 }

                 if (angular.isDefined(data) && verb.match(/post|put/)) {
                   config.data = data;
                 }

                 $http(config).then(function (data) {
                     defer.resolve(data.data);
                   },
                   function (error) {
                     var rejected = {
                       status: error.status,
                       statusText: error.statusText,
                       message: error.data.message
                     };

                     var callback = defer.promise.$$state.pending;
                     if (callback && callback.length && typeof callback[0][2] !== 'function') {
                       NotificationUtil.error(rejected);
                     }

                     defer.reject(rejected);
                   });

                 return defer.promise;
               }

               function rewriteUri(uri) {
                 if (uri.startsWith('/')) {
                   return uri;
                 } else {
                   return CentralDogmaConstant.API_PREFIX + uri;
                 }
               }

               return {
                 get: function (uri, config) {
                   return makeRequest('get', uri, config);
                 },

                 post: function (uri, data, config) {
                   return makeRequest('post', uri, config, data);
                 },

                 put: function (uri, data, config) {
                   return makeRequest('put', uri, config, data);
                 },

                 delete: function (uri, config) {
                   return makeRequest('delete', uri, config);
                 }
               };
             });
