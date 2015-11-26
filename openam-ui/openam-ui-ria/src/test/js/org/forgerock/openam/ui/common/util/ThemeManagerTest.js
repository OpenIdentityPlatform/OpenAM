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

define([
    "jquery",
    "lodash",
    "squire",
    "sinon",
    "org/forgerock/openam/ui/common/util/Constants"
], function ($, _, Squire, sinon, Constants) {
    var baseUrl = "toUrl:",
        ThemeManager, Configuration, EventManager, URIUtils, Router,
        mock$, themeConfig, urlParams, sandbox;
    describe("org/forgerock/openam/ui/common/util/ThemeManager", function () {
        beforeEach(function (done) {
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

            sandbox = sinon.sandbox.create();
            sandbox.stub(require, "toUrl", function (url) {
                return baseUrl + url;
            });

            Configuration = {
                globalData: {
                    theme: undefined,
                    realm: "/"
                }
            };

            EventManager = {
                sendEvent: sinon.stub()
            };

            URIUtils = {
                getCurrentCompositeQueryString: sinon.stub().returns(""),
                parseQueryString: sinon.stub().returns(urlParams)
            };

            Router = {
                currentRoute: {}
            };

            injector
                .mock("jquery", mock$)
                .mock("config/ThemeConfiguration", themeConfig)
                .mock("org/forgerock/commons/ui/common/util/URIUtils", URIUtils)
                .mock("org/forgerock/commons/ui/common/main/Configuration", Configuration)
                .mock("org/forgerock/commons/ui/common/main/EventManager", EventManager)
                .mock("Router", Router)
                .require(["org/forgerock/openam/ui/common/util/ThemeManager"], function (d) {
                    ThemeManager = d;
                    done();
                });
        });

        afterEach(function () {
            sandbox.restore();
        });

        describe("#getTheme", function () {
            it("sends EVENT_THEME_CHANGED event", function () {
                return ThemeManager.getTheme().then(function () {
                    expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_THEME_CHANGED);
                });
            });
            it("throws if theme configuration does not contain a theme object", function () {
                delete themeConfig.themes;
                expect(function () {
                    ThemeManager.getTheme();
                }).to.throw();
            });
            it("throws if theme configuration does specify a default theme", function () {
                delete themeConfig.themes.default;
                expect(function () {
                    ThemeManager.getTheme();
                }).to.throw();
            });
            it("returns a promise", function (done) {
                var result = ThemeManager.getTheme();
                expect(result.then).to.not.be.undefined;
                result.then(function () {
                    done();
                });
            });
            it("places the selected theme onto the global data object", function () {
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.default);
                });
            });
            it("selects the correct theme based on the realm", function () {
                Configuration.globalData.realm = "/b";
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the correct theme based on the realm", function () {
                Configuration.globalData.realm = "/b";
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the default theme if no realms match", function () {
                Configuration.globalData.realm = "/c";
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.default);
                });
            });
            it("allows mappings to specify regular expressions to match realms", function () {
                themeConfig.mappings[0].realms[0] = /^\/hello.*/;
                Configuration.globalData.realm = "/hello/world";
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the correct theme based on the authentication chain", function () {
                urlParams.service = "test";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: ["test"]
                });
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the default theme if no authentication chains match", function () {
                urlParams.service = "tester";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: ["test"]
                });
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.default);
                });
            });
            it("allows mappings to specify regular expressions to match authentication chains", function () {
                urlParams.service = "tester";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: [/test/]
                });
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("matches realms and authentication chains if both are specified in a mapping", function () {
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
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("won't match a mapping that needs an authentication chain if none is present", function () {
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
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("matches a mapping that has an empty authentication chain if none is present", function () {
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: [""]
                });
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("fills in any missing properties from selected theme with the default theme", function () {
                Configuration.globalData.realm = "/b";
                delete themeConfig.themes.other.stylesheets;
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme.stylesheets)
                        .to.deep.equal(themeConfig.themes.default.stylesheets);
                });
            });
            it("doesn't try to merge arrays in the selected theme with the default theme", function () {
                Configuration.globalData.realm = "/b";
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme.stylesheets)
                        .to.deep.equal(themeConfig.themes.other.stylesheets);
                });
            });
            it("updates src fields in the theme to be relative to the entry point", function () {
                themeConfig.themes.default.settings = {
                    logo: {
                        src: "foo"
                    },
                    loginLogo: {
                        src: "bar"
                    }
                };
                return ThemeManager.getTheme().then(function () {
                    expect(Configuration.globalData.theme.settings).to.deep.equal({
                        logo: {
                            src: baseUrl + "foo"
                        },
                        loginLogo: {
                            src: baseUrl + "bar"
                        }
                    });
                });
            });
            it("removes any existing CSS and favicons from the page", function () {
                return ThemeManager.getTheme().then(function () {
                    expect(mock$).to.be.calledWith("link");
                    sinon.assert.calledOnce(mock$.remove);
                });
            });
            it("adds the favicon to the page", function () {
                return ThemeManager.getTheme().then(function () {
                    expect(mock$).to.be.calledWith("<link/>", {
                        rel: "icon",
                        type: "image/x-icon",
                        href: baseUrl + "icon.png"
                    });
                    expect(mock$.appendTo).to.be.calledWith("head");
                });
            });
            it("adds the alternate favicon to the page", function () {
                return ThemeManager.getTheme().then(function () {
                    expect(mock$).to.be.calledWith("<link/>", {
                        rel: "shortcut icon",
                        type: "image/x-icon",
                        href: baseUrl + "icon.png"
                    });
                    expect(mock$.appendTo).to.be.calledWith("head");
                });
            });
            it("adds any stylesheets to the page", function () {
                return ThemeManager.getTheme().then(function () {
                    expect(mock$).to.be.calledWith("<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: baseUrl + "a.css"
                    });
                    expect(mock$).to.be.calledWith("<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: baseUrl + "c.css"
                    });
                    expect(mock$.appendTo).to.be.calledWith("head");
                });
            });
            it("doesn't update the page if the theme hasn't changed since the last call", function () {
                return ThemeManager.getTheme().then(function () {
                    mock$.reset();
                    return ThemeManager.getTheme();
                }).then(function () {
                    expect(mock$).to.not.be.called;
                });
            });
            it("overrides the theme's stylesheets if the user is on an admin page", function () {
                Router.currentRoute.navGroup = "admin";
                return ThemeManager.getTheme().then(function () {
                    expect(mock$).to.be.calledWith("<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: baseUrl + Constants.DEFAULT_STYLESHEETS[0]
                    });
                    expect(mock$).to.be.calledWith("<link/>", {
                        rel: "stylesheet",
                        type: "text/css",
                        href: baseUrl + Constants.DEFAULT_STYLESHEETS[1]
                    });
                });
            });
        });
    });
});
