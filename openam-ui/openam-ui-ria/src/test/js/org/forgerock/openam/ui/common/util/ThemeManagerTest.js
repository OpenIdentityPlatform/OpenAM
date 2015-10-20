/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

/*global define, QUnit*/

define("org/forgerock/openam/ui/common/util/ThemeManagerTest", [
    "jquery",
    "underscore",
    "squire",
    "sinon",
    "org/forgerock/openam/ui/common/util/Constants"
], function ($, _, Squire, sinon, Constants) {
    return {
        executeAll: function () {
            var ThemeManager, Configuration, Router, mock$, themeConfig, urlParams;
            QUnit.module("org/forgerock/openam/ui/common/util/ThemeManager", {
                setup: function () {
                    var injector = new Squire();

                    themeConfig = {
                        themes: {
                            "default": {
                                path: "",
                                icon: "icon.png",
                                stylesheets: ["a.css", "c.css"]
                            },
                            other: {
                                name: "other",
                                path: "",
                                icon: "otherIcon.png",
                                stylesheets: ["b.css"]
                            }
                        },
                        mappings: [
                            { theme: "other", realms: ["/b"] }
                        ]
                    };

                    urlParams = {};

                    mock$ = sinon.spy(function () { return mock$; });
                    mock$.remove = sinon.spy();
                    mock$.appendTo = sinon.spy();
                    mock$.Deferred = _.bind($.Deferred, $);

                    URIUtils = {
                        getCurrentCompositeQueryString: sinon.stub().returns(""),
                        parseQueryString: sinon.stub().returns(urlParams)
                    };

                    QUnit.stop();
                    injector
                        .mock("jquery", mock$)
                        .mock("config/ThemeConfiguration", themeConfig)
                        .mock("org/forgerock/commons/ui/common/util/URIUtils", URIUtils)
                        .store("org/forgerock/commons/ui/common/main/Configuration")
                        .require(["org/forgerock/openam/ui/common/util/ThemeManager", "mocks"], function (d, mocks) {
                            ThemeManager = d;
                            Configuration = mocks.store["org/forgerock/commons/ui/common/main/Configuration"];
                            Configuration.globalData = {
                                theme: undefined,
                                realm: "/"
                            };
                            QUnit.start();
                        });
                }
            });

            test("getTheme throws if theme configuration does not contain a theme object", function () {
                delete themeConfig.themes;
                throws(function () {
                    ThemeManager.getTheme();
                }, "Theme configuration must specify a themes object");
            });
            test("getTheme throws if theme configuration does specify a default theme", function () {
                delete themeConfig.themes.default;
                throws(function () {
                    ThemeManager.getTheme();
                }, "Theme configuration must specify a default theme");
            });
            test("getTheme returns a promise", function () {
                QUnit.stop();
                var result = ThemeManager.getTheme();
                ok(result.then, "returned object has a then property");
                result.then(function () {
                    QUnit.start();
                });
            });
            test("getTheme places the selected theme onto the global data object", function () {
                QUnit.stop();
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.default, "saved default theme");
                    QUnit.start();
                });
            });
            test("getTheme selects the correct theme based on the realm", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/b";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme selects the correct theme based on the realm", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/b";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme selects the default theme if no realms match", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/c";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.default, "selected default theme");
                    QUnit.start();
                });
            });
            test("getTheme allows mappings to specify regular expressions to match realms", function () {
                QUnit.stop();
                themeConfig.mappings[0].realms[0] = /^\/hello.*/;
                Configuration.globalData.realm = "/hello/world";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme selects the correct theme based on the authentication chain", function () {
                QUnit.stop();
                urlParams.service = "test";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: ["test"]
                });
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme selects the default theme if no authentication chains match", function () {
                QUnit.stop();
                urlParams.service = "tester";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: ["test"]
                });
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.default, "selected default theme");
                    QUnit.start();
                });
            });
            test("getTheme allows mappings to specify regular expressions to match authentication chains", function () {
                QUnit.stop();
                urlParams.service = "tester";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: [/test/]
                });
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme matches realms and authentication chains if both are specified in a mapping", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/a";
                urlParams.service = "test";
                // No match - wrong realm
                themeConfig.mappings.push({
                    theme: "default",
                    realms: ["/b"],
                    authenticationChains: ["test"]
                });
                // No match - wrong authentication chain
                themeConfig.mappings.push({
                    theme: "default",
                    realms: ["/a"],
                    authenticationChains: ["tester"]
                });
                // Match
                themeConfig.mappings.push({
                    theme: "other",
                    realms: ["/a"],
                    authenticationChains: ["test"]
                });
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme won't match a mapping that needs an authentication chain if none is present", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/a";
                // No match - wants an authentication chain but none is present
                themeConfig.mappings.push({
                    theme: "default",
                    realms: ["/a"],
                    authenticationChains: ["test"]
                });
                // Match
                themeConfig.mappings.push({
                    theme: "other",
                    realms: ["/a"]
                });
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme matches a mapping that has an empty authentication chain if none is present", function () {
                QUnit.stop();
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: [""]
                });
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes.other, "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme fills in any missing properties from selected theme with the default theme", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/b";
                delete themeConfig.themes.other.stylesheets;
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme.stylesheets, themeConfig.themes.default.stylesheets,
                        "stylesheets comes from default theme");
                    QUnit.start();
                });
            });
            test("getTheme doesn't try to merge arrays in the selected theme with the default theme", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/b";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme.stylesheets, themeConfig.themes.other.stylesheets,
                        "stylesheets are unmerged");
                    QUnit.start();
                });
            });
            test("getTheme updates src fields in the theme to be relative to the entry point", function () {
                QUnit.stop();
                themeConfig.themes.default.settings = {
                    logo: {
                        src: "foo"
                    },
                    loginLogo: {
                        src: "bar"
                    }
                };
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme.settings, {
                        logo: {
                            src: "./foo"
                        },
                        loginLogo: {
                            src: "./bar"
                        }
                    }, "URLs are made relative");
                    QUnit.start();
                });
            });
            test("getTheme removes any existing CSS and favicons from the page", function () {
                QUnit.stop();
                expect(0);
                ThemeManager.getTheme().then(function () {
                    sinon.assert.calledWith(mock$, "link");
                    sinon.assert.calledOnce(mock$.remove);
                    QUnit.start();
                });
            });
            test("getTheme adds the favicon to the page", function () {
                QUnit.stop();
                expect(0);
                ThemeManager.getTheme().then(function () {
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "icon",
                        type: "image/x-icon",
                        href: "./icon.png"
                    });
                    sinon.assert.calledWith(mock$.appendTo, "head");
                    QUnit.start();
                });
            });
            test("getTheme adds the alternate favicon to the page", function () {
                QUnit.stop();
                expect(0);
                ThemeManager.getTheme().then(function () {
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "shortcut icon",
                        type: "image/x-icon",
                        href: "./icon.png"
                    });
                    sinon.assert.calledWith(mock$.appendTo, "head");
                    QUnit.start();
                });
            });
            test("getTheme adds any stylesheets to the page", function () {
                QUnit.stop();
                expect(0);
                ThemeManager.getTheme().then(function () {
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "./a.css"
                    });
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "./c.css"
                    });
                    sinon.assert.calledWith(mock$.appendTo, "head");
                    QUnit.start();
                });
            });
            test("getTheme doesn't update the page if the theme hasn't changed since the last call", function () {
                QUnit.stop();
                expect(0);
                ThemeManager.getTheme().then(function () {
                    mock$.reset();
                    return ThemeManager.getTheme();
                }).then(function () {
                    sinon.assert.notCalled(mock$);
                    QUnit.start();
                });
            });
            test("getTheme always updates the page if force is true", function () {
                QUnit.stop();
                expect(0);
                ThemeManager.getTheme().then(function () {
                    mock$.reset();
                    return ThemeManager.getTheme(true);
                }).then(function () {
                    sinon.assert.called(mock$);
                    QUnit.start();
                });
            });
            test("getTheme overrides the theme's stylesheets if the user is an admin", function () {
                QUnit.stop();
                expect(0);
                Configuration.loggedUser = {
                    roles: "ui-admin"
                };
                ThemeManager.getTheme().then(function () {
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "./" + Constants.DEFAULT_STYLESHEETS[0]
                    });
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "./" + Constants.DEFAULT_STYLESHEETS[1]
                    });
                    QUnit.start();
                });
            });
        }
    };
});
