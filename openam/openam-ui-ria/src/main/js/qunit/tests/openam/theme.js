/*global require, define, module, $, QUnit, window*/
define([
    "org/forgerock/openam/ui/user/login/RESTLoginView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "sinon",
    "mocks/lessRequests",
    "mocks/openam/theming"
], function (
    LoginView,
    conf,
    sinon,
    lessRequests,
    theming) {

        return {
            executeAll: function (server) {

                module("Theming Tests",{
                    setup: function(){
                        lessRequests(server);
                        conf.baseTemplate = "";
                        delete window.location.hash;
                    },
                    teardown: function(){
                        delete window.location.hash;
                    }
                });

                QUnit.test("Check Theme by Realm", function () {

                    theming(server);
                    
                    conf.globalData.auth.realm = "/test/";

                    LoginView.render(["/test/"]);

                    server.respond();
                    
                    QUnit.ok($("#footer:contains('test@test.com')").length === 1, "Footer Changed");

                    QUnit.ok($("img[alt=ForgeRock_test]").length === 1, "Logo 'alt' Attribute Changed");

                    QUnit.ok($("img[height=80]").length === 1, "Logo 'width' Attribute Changed");

                    QUnit.ok($("img[src='images/new_logo.png']").length === 1, "Logo Image Changed");
                });

            }
        };

});