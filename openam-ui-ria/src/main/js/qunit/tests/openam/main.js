/*global require, define, module, $, QUnit, window*/
define([
    "tests/openam/login",
    "tests/openam/theme"
], function (login,theme) {

    return {
        executeAll: function (server) {
            QUnit.start();
            QUnit.testDone(function( details ) {
                server.responses = [];
                delete window.location.hash;
            });
            QUnit.done(function () {
                delete window.location.hash;
               console.log("QUNIT DONE");
            });

            login.executeAll(server);

            theme.executeAll(server);
        }
    };

});