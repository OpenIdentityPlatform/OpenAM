/*global require, define, module, $, QUnit, window*/
define([
    "tests/openam/login",
    "tests/openam/theme",
    "tests/openam/loginUrlParams"
], function (login,theme,loginUrlParams) {

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

            $.ajaxSetup({async: false});

            login.executeAll(server);

            theme.executeAll(server);
            
            loginUrlParams.executeAll(server);
        }
    };

});