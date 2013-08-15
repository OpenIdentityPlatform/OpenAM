/*global require, define, module, $, QUnit, window*/
define([
    "org/forgerock/openam/ui/user/login/RESTLoginView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "sinon",
    "mocks/lessRequests",
    "mocks/openam/invalidLoginCredentials"
], function (
    LoginView,
    conf,
    sinon,
    lessRequests,
    invalidLoginCredentials) {

        return {
            executeAll: function (server) {

                module("Login Tests",{
                    setup: function(){
                        lessRequests(server);
                        conf.baseTemplate = "";
                        delete window.location.hash;
                    },
                    teardown: function(){
                        delete window.location.hash;
                    }
                });
                
                

                QUnit.test("Invalid Login Credentials", function () {

                    invalidLoginCredentials(server);

                    LoginView.render(["/",undefined]);

                    server.respond();
                    
                    QUnit.ok($('[name="callback_0"]').length, "Login Input Displayed");
                    
                    $("[name=callback_0]").val("foo").trigger("change");

                    $(':submit').trigger("click");

                    server.respond();

                    QUnit.ok($(".errorMessage:contains('Login/password combination is invalid')").length === 1, "Invalid Credentials");


                });

            }
        };

});