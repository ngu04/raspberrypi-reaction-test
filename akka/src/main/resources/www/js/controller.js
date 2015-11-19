var webClient = angular.module('webClient', []);

webClient.controller('Controller', function ($scope) {

    var webSocket = new WebSocket('ws://localhost:8080/ws');
    webSocket.onmessage = function(message) {
        $scope.$apply(function() {
            var data = JSON.parse(message.data);
            switch(data.type) {
                case "players":
                    $scope.players = data.players;
                    break;
            }
        });
    };

    $scope.formDisabled = false;

    $scope.players = [];

    $scope.play = function(){
        $scope.formDisabled = true;
        var json = angular.toJson({ type: 'play', email: angular.lowercase($scope.email) });
        $scope.email = '';
        $scope.webSocket.send(json);
    };
});
