var webClient = angular.module('webClient', ['ngRoute']);

webClient.controller('Controller', function ($scope, $log) {

    var webSocket = new ReconnectingWebSocket('ws://localhost:8080/ws');
    webSocket.maxReconnectInterval = 3000;
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
    webSocket.onopen = function() {
        $scope.$apply(function() {
            $scope.connected = true;
        });
    };
    webSocket.onclose = function() {
        $scope.$apply(function() {
            $scope.connected = false;
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
                templateUrl: 'views/start.html'
            })
            .when('/results', {
                templateUrl: 'views/results.html'
            })
            .otherwise({
                redirectTo: '/start'
            });
    });