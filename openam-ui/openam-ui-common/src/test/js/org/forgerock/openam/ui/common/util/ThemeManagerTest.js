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
    "squire",
    "sinon",
    "org/forgerock/openam/ui/common/util/Constants"
], function ($, Squire, sinon, Constants) {
    return {
        executeAll: function () {
            var ThemeManager, Configuration, mock$, themeConfig;
            QUnit.module("org/forgerock/openam/ui/common/util/ThemeManager", {
                setup: function () {
                    var injector = new Squire(),
                        fetchConfigPromise = $.Deferred();

                    themeConfig = {
                        themes: [{
                            name: "default",
                            path: "",
                            realms: ["/"],
                            regex: false,
                            icon: "icon.png",
                            stylesheets: ["a.css", "c.css"]
                        }, {
                            name: "other",
                            path: "",
                            realms: ["/b"],
                            regex: false,
                            icon: "otherIcon.png",
                            stylesheets: ["b.css"]
                        }]
                    };

                    mock$ = sinon.spy(function () { return mock$; });
                    mock$.getJSON = sinon.stub().returns(fetchConfigPromise.promise());
                    mock$.remove = sinon.spy();
                    mock$.appendTo = sinon.spy();
                    fetchConfigPromise.resolve(themeConfig);

                    QUnit.stop();
                    injector
                        .mock("jquery", mock$)
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

            test("getTheme returns a promise", function () {
                QUnit.stop();
                var result = ThemeManager.getTheme();
                ok(result.then, "returned object has a then property");
                result.then(function () {
                    QUnit.start();
                });
            });
            test("getTheme fetches the theme config as a relative URL", function () {
                QUnit.stop();
                expect(0);
                ThemeManager.getTheme().then(function () {
                    sinon.assert.calledWith(mock$.getJSON, "./" + Constants.THEME_CONFIG_PATH);
                    QUnit.start();
                });
            });
            test("getTheme places the selected theme onto the global data object", function () {
                QUnit.stop();
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes[0], "saved default theme");
                    QUnit.start();
                });
            });
            test("getTheme selects the correct theme based on the realm", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/b";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes[1], "selected other theme");
                    QUnit.start();
                });
            });
            test("getTheme selects the default theme if no realms match", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/c";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme, themeConfig.themes[0], "selected default theme");
                    QUnit.start();
                });
            });
            test("getTheme fills in any missing properties from selected theme with the default theme", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/b";
                delete themeConfig.themes[1].stylesheets;
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme.stylesheets, themeConfig.themes[0].stylesheets,
                        "stylesheets comes from default theme");
                    QUnit.start();
                });
            });
            test("getTheme doesn't try to merge arrays in the selected theme with the default theme", function () {
                QUnit.stop();
                Configuration.globalData.realm = "/b";
                ThemeManager.getTheme().then(function () {
                    deepEqual(Configuration.globalData.theme.stylesheets, themeConfig.themes[1].stylesheets,
                        "stylesheets are unmerged");
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
                        href: "a.css"
                    });
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "c.css"
                    });
                    sinon.assert.calledWith(mock$.appendTo, "head");
                    QUnit.start();
                });
            });
            test("getTheme prepends basePath if stylesheet URLs are not absolute", function () {
                QUnit.stop();
                expect(0);
                themeConfig.themes[0].stylesheets = ["a/b.css", "/c/d.css", "http://example.com:8080/my.css"];
                ThemeManager.getTheme("base/path/").then(function () {
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "base/path/a/b.css"
                    });
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "/c/d.css"
                    });
                    sinon.assert.calledWith(mock$, "<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: "http://example.com:8080/my.css"
                    });
                    sinon.assert.calledWith(mock$.appendTo, "head");
                    QUnit.start();
                });
            });
        }
    };
});
