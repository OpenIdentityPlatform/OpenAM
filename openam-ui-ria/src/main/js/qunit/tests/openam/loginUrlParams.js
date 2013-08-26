/*global require, define, module, $, QUnit, window*/
define([
    "org/forgerock/openam/ui/user/login/RESTLoginView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "sinon",
    "mocks/lessRequests",
    "mocks/openam/loginUrlParams/common",
    "mocks/openam/loginUrlParams/goto",
    "mocks/openam/loginUrlParams/gotoOnFail",
    "mocks/openam/loginUrlParams/realm",
    "mocks/openam/loginUrlParams/user",
    "mocks/openam/loginUrlParams/locale",
    "mocks/openam/loginUrlParams/module",
    "mocks/openam/loginUrlParams/service",
    "mocks/openam/loginUrlParams/arg",
    "mocks/openam/loginUrlParams/authlevel",
    "mocks/openam/loginUrlParams/forceAuth",
    "mocks/openam/loginUrlParams/IDToken"
], function (
    LoginView,
    conf,
    cookieHelper,
    sessionManager,
    sinon,
    lessRequests,
    common,
    gotoMock,
    gotoOnFail,
    realm,
    user,
    locale,
    mod,
    service,
    arg,
    authlevel,
    forceAuth,
    iDToken) {

        return {
            executeAll: function (server) {

                module("Login Url Param Tests",{
                    setup: function(){
                        lessRequests(server);
                        common(server);
                        conf.baseTemplate = "";
                        conf.setProperty('gotoURL', null);
                        delete window.location.hash;
                    },
                    teardown: function(){
                        history.pushState("", document.title, window.location.pathname + window.location.search);
                        delete window.location.hash;
                    }
                });

                QUnit.test("goto", function () {

                    gotoMock(server);
                    
                    window.location.hash = "&goto=%23gotoTest";

                    LoginView.render(["/","&goto=%23gotoTest"]);
                    
                    QUnit.ok(!conf.loggedUser, "success...not currently logged in");
                    
                    $("[name=callback_0]").val("demo").trigger("change");
                    
                    $("[name=callback_1]").val("changeit").trigger("change");

                    $(':submit').trigger("click");
                    
                    QUnit.ok(conf.loggedUser, "success...user logged in");
                    
                    QUnit.ok(window.location.href.indexOf("#gotoTest") > -1, "success...navigation to goto param url after valid login");
                    
                    sessionManager.logout();
                    
                    conf.setProperty('loggedUser', null);

                });

                QUnit.test("gotoOnFail", function () {

                    gotoOnFail(server);
                    
                    window.location.hash = "&gotoOnFail=%23gotoOnFailTest";

                    LoginView.render(["/","&gotoOnFail=%23gotoOnFailTest"]);
                    
                    $("[name=callback_0]").val("demo").trigger("change");

                    $(':submit').trigger("click");
                    
                    QUnit.ok(window.location.href.indexOf("#gotoOnFailTest") > -1, "success...navigation to gotoOnFail param url after failed login");
                });

                QUnit.test("realm", function () {

                    realm(server);
                    
                    window.location.hash = "&realm=%2Ftest%2F";

                    LoginView.render(["/","&realm=%2Ftest%2F"]);
                    
                    QUnit.ok(conf.globalData.auth.realm.indexOf('test') > -1, "success...realm param posted to authenticate rest call");
                });

                QUnit.test("user", function () {

                    user(server);
                    
                    window.location.hash = "&user=demo";

                    LoginView.render(["/","&user=demo"]);
                    
                    QUnit.equal(conf.globalData.auth.urlParams.authIndexType, "user", "success...user param posted to authenticate rest call");
                });

                QUnit.test("service", function () {

                    service(server);
                    
                    window.location.hash = "&service=ldapService";

                    LoginView.render(["/","&service=ldapService"]);
                    
                    QUnit.equal(conf.globalData.auth.urlParams.authIndexType, "service", "success...service param posted to authenticate rest call");
                });

                QUnit.test("authlevel", function () {

                    authlevel(server);
                    
                    window.location.hash = "&authlevel=2";

                    LoginView.render(["/","&authlevel=2"]);
                    
                    QUnit.ok(conf.globalData.auth.additional.indexOf('authIndexType=level') > -1, "success...authlevel param posted to authenticate rest call");
                });

                QUnit.test("module", function () {

                    mod(server);
                    
                    window.location.hash = "&module=OATH";

                    LoginView.render(["/","&module=OATH"]);
                    
                    QUnit.equal($(":submit").val(),"Submit OTP Code", "success...module param changes login view to use correct auth module");
                });

                QUnit.test("arg", function () {
                    
                    gotoMock(server);

                    arg(server);
                    
                    window.location.hash = "#";

                    LoginView.render(["/"]);
                    
                    QUnit.ok(!conf.loggedUser, "success...not currently logged in");
                    
                    $("[name=callback_0]").val("demo").trigger("change");
                    
                    $("[name=callback_1]").val("changeit").trigger("change");

                    $(':submit').trigger("click");
                    
                    QUnit.ok(conf.loggedUser, "success...user logged in");

                    window.location.hash = "&arg=newsession";
                    
                    LoginView.render(['/','&arg=newsession']);
                    
                    QUnit.ok(!cookieHelper.getCookie('iPlanetDirectoryPro'), "success...arg=newsession passed in, user logged out and session cleared");
                    
                    sessionManager.logout();
                    
                    conf.setProperty('loggedUser', null);
                    
                });

                QUnit.test("ForceAuth", function () {
                    
                    gotoMock(server);

                    forceAuth(server);
                    
                    window.location.hash = "#";

                    LoginView.render(["/"]);
                    
                    QUnit.ok(!conf.loggedUser, "success...not currently logged in");
                    
                    $("[name=callback_0]").val("demo").trigger("change");
                    
                    $("[name=callback_1]").val("changeit").trigger("change");

                    $(':submit').trigger("click");
                    
                    QUnit.ok(conf.loggedUser, "success...user logged in");
                    
                    var spy = sinon.spy(sessionManager,'getLoggedUser');

                    window.location.hash = "&ForceAuth=true";
                    
                    LoginView.render(['/','&ForceAuth=true']);
                    
                    QUnit.ok(!spy.called && conf.loggedUser, "success...ForceAuth=true passed in, user still has active session");
                    
                    sessionManager.logout();
                    
                    conf.setProperty('loggedUser', null);
                });

                QUnit.test("IDToken", function () {
                    var autoLoginSpy = sinon.spy(LoginView,'autoLogin'),
                        configSetPropertySpy = sinon.spy(conf,'setProperty');
                    
                    QUnit.ok(!conf.loggedUser, "success...not currently logged in");

                    iDToken(server);
                    
                    window.location.hash = "&IDToken1=demo&IDToken2=changeit";

                    LoginView.render(["/","&IDToken1=demo&IDToken2=changeit"]);
                    
                    QUnit.ok(autoLoginSpy.called && configSetPropertySpy.firstCall.args[1].cn === "demo", "success...IDToken params passed in, user is automatically logged in.");
                    
                    sessionManager.logout();
                    
                    conf.setProperty('loggedUser', null);
                });

                QUnit.test("locale", function () {
                    $.ajaxSetup({async: true});

                    locale(server);
                    
                    window.location.hash = "&locale=fr";

                    LoginView.render(["/","&locale=fr"]);
                    
                    server.respond();
                    
                    QUnit.ok($("label:contains('Mot de passe')").size() > 0, "success...server locale change");
                    
                    QUnit.equal($(":submit").val(),"Login et vous", "success...client locale change");
                    
                    cookieHelper.deleteCookie('i18next','/');

                    $.ajaxSetup({async: false});
                });

            }
        };

});