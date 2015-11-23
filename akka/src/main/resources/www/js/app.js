var webClient = angular.module('webClient', ['ngRoute']);

webClient.controller('Controller', function ($scope, $log) {

    function createWebSocket(webSocketUrl) {
        var webSocket = new ReconnectingWebSocket(webSocketUrl);
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
                        $scope.leaderBoard = groupByName(data.leaderBoard);
                        break;
                }
            });
        };

        webSocket.onopen = function() {
            $scope.$apply(function() {
                $scope.connected = true;
                $scope.user = {};
            });
        };
        webSocket.onclose = function() {
            $scope.$apply(function() {
                $scope.connected = false;
            });
        };

        return webSocket;
    }

    function groupByName(results) {
        var grouped = {};
        for(var i = 0; i < results.length; i++) {
            var name = results[i].name;
            var score = results[i].score;
            if(grouped[name]) {
                grouped[name].bestScore = Math.max(grouped[name].bestScore, score);
                grouped[name].results.push(results[i]);
            } else {
                grouped[name] = {
                    name: name,
                    bestScore: score,
                    results: [ results[i] ]
                };
            }
        }
        var ret = [];
        for (var group in grouped) {
            ret.push(grouped[group]);
        }
        return ret;
    }

    $scope.user = {};
    $scope.leaderBoard = [];
    $scope.waitingStartSignal = false;
    $scope.gameInProgress = false;

    $scope.webSocketUrl = 'ws://localhost:8080/ws';

    var webSocket = createWebSocket($scope.webSocketUrl);

    $scope.setWebSocketUrl = function(webSocketUrl) {
        webSocket.close();
        webSocket = createWebSocket(webSocketUrl);
    };


    $scope.disableRegistration = function () {
        return $scope.waitingStartSignal || $scope.gameInProgress;
    };

    $scope.register = function(){
        $scope.formDisabled = true;
        $log.info($scope.user)
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