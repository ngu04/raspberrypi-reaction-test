var webClient = angular.module('webClient', ['ngRoute', 'ui.bootstrap']);

webClient.controller('Controller', function ($scope, $location, $log) {

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
                        $scope.currentResult = {};
                        break;
                    case "gameInProgress":
                        $scope.waitingStartSignal = false;
                        $scope.gameInProgress = true;
                        break;
                    case "currentResult":
                        $scope.currentResult = data.currentResult;
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
                $scope.currentResult = {};
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
            var nickName = results[i].nickName;
            var score = results[i].score;
            var email = results[i].email;
            if(grouped[nickName]) {
                grouped[nickName].bestScore = Math.max(grouped[nickName].bestScore, score);
                grouped[nickName].results.push(results[i]);
            } else {
                grouped[nickName] = {
                    nickName: nickName,
                    email: email,
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
    $scope.currentResult = {};
    $scope.leaderBoard = [];
    $scope.waitingStartSignal = false;
    $scope.gameInProgress = false;

    $scope.webSocketUrl = 'ws://' + $location.host() + ':' + $location.port() + '/ws';

    var webSocket = createWebSocket($scope.webSocketUrl);

    $scope.onSelectName = function($item){
        $scope.user.email = $item.email;
    };

    $scope.setWebSocketUrl = function(webSocketUrl) {
        webSocket.close();
        webSocket = createWebSocket(webSocketUrl);
    };

    $scope.disableRegistration = function () {
        return $scope.waitingStartSignal || $scope.gameInProgress;
    };

    $scope.showCurrentResult = function () {
        return typeof $scope.currentResult.nickName !== "undefined";
    };

    $scope.emotion = function () {
        if($scope.currentResult.score < 200) return "What to say. Maybe next time.";
        else if ($scope.currentResult.score < 400)return "Not bad but you can do more!"
        else if ($scope.currentResult.score < 700)return "Nice one."
        else if ($scope.currentResult.score < 1000)return "Congratulations!"
        else if ($scope.currentResult.score < 1300)return "Boooom! This is how it goes!"
        else if ($scope.currentResult.score < 1600)return "Unbeatable!"
        else if ($scope.currentResult.score < 2000)return "God mode!"
        else return "How did you do this??!!"
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