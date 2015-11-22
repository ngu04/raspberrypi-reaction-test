var webClient = angular.module('webClient', ['ngRoute']);

webClient.controller('Controller', function ($scope, $log) {

    var webSocket = new WebSocket('ws://localhost:8080/ws');
    webSocket.onmessage = function(message) {
        $scope.$apply(function() {
            var data = JSON.parse(message.data);
            $log.info(data)
            switch(data.type) {
                case "waitingStartSignal":
                    $scope.waitingStartSignal = true;
                    $scope.gameInProgress = false;
                    break;
                case "openUserRegistration":
                    $scope.waitingStartSignal = false;
                    $scope.gameInProgress = false;
                    break;
                case "gameInProgress":
                    $scope.waitingStartSignal = false;
                    $scope.gameInProgress = true;
                    break;
                case "leaderBoard":
                    $scope.leaderBoard = data.leaderBoard;
                    break;
            }
        });
    };

    $scope.waitingStartSignal = false;

    $scope.gameInProgress = false;

    $scope.disableRegistration = function () {
        return $scope.waitingStartSignal || $scope.gameInProgress;
    };

    $scope.leaderBoard = [];

    $scope.register = function(){
        $scope.formDisabled = true;
        var message = angular.toJson({
            type: 'user',
            user: $scope.user
        });
        $scope.user = {};
        webSocket.send(message);
    };
});

webClient.config(function ($routeProvider) {
        $routeProvider
            .when('/start', {
                templateUrl: 'views/start.html',
                controller: 'Controller'
            })
            .when('/results', {
                templateUrl: 'views/results.html',
                controller: 'Controller'
            })
            .otherwise({
                redirectTo: '/start'
            });
    });