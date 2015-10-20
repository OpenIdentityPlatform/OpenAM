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

define("org/forgerock/openam/ui/common/util/RealmHelperTest", [
    "squire"
], function (Squire) {
    return {
        executeAll: function () {

            var injector = new Squire(),
                URIUtils,
                Configuration;

            injector
                .store("org/forgerock/commons/ui/common/main/Configuration")
                .store("org/forgerock/commons/ui/common/util/URIUtils")
                .require(["org/forgerock/openam/ui/common/util/RealmHelper", "mocks"], function (RealmHelper, mocks) {

                    QUnit.module("org/forgerock/openam/ui/common/util/RealmHelper", {
                        setup: function () {
                            URIUtils = mocks.store["org/forgerock/commons/ui/common/util/URIUtils"];
                            Configuration = mocks.store["org/forgerock/commons/ui/common/main/Configuration"];

                            Configuration.globalData = {
                                auth: {
                                    subRealm: undefined
                                }
                            };
                        }
                    });

                    test("#decorateURLWithOverrideRealm", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentQueryString").returns("realm=realm1");

                        equal(RealmHelper.decorateURLWithOverrideRealm("http://www.example.com"),
                            "http://www.example.com?realm=realm1", "appends override realm query string parameter");
                    }));

                    test("#decorateURLWithOverrideRealm when a query string is present", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentQueryString").returns("realm=realm1");

                        equal(RealmHelper.decorateURLWithOverrideRealm("http://www.example.com?key=value"),
                            "http://www.example.com?key=value&realm=realm1",
                            "appends override realm query string parameter");
                    }));

                    test("#decorateURIWithRealm", sinon.test(function () {
                        Configuration.globalData.auth.subRealm = "realm1";
                        this.stub(URIUtils, "getCurrentQueryString").returns("realm=realm2");

                        equal(RealmHelper.decorateURIWithRealm("http://www.example.com/__subrealm__/"),
                            "http://www.example.com/realm1/?realm=realm2", "replaces __subrealm__ with sub realm and " +
                                "appends override realm query string parameter");
                    }));

                    test("#decorateURIWithSubRealm", sinon.test(function () {
                        Configuration.globalData.auth.subRealm = "realm1";

                        equal(RealmHelper.decorateURIWithSubRealm("http://www.example.com/__subrealm__/"),
                            "http://www.example.com/realm1/", "replaces __subrealm__ with sub realm");
                    }));

                    test("#decorateURIWithSubRealm when there is not sub realm", sinon.test(function () {
                        Configuration.globalData.auth.subRealm = "";

                        equal(RealmHelper.decorateURIWithSubRealm("http://www.example.com/__subrealm__/"),
                            "http://www.example.com/", "removes __subrealm__");
                    }));

                    test("#getOverrideRealm when realm override is present in query string", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentQueryString").returns("realm=realm1");

                        equal(RealmHelper.getOverrideRealm(), "realm1", "returns override realm");
                    }));

                    test("#getOverrideRealm when realm override is present in fragment query string", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentFragmentQueryString").returns("realm=realm1");

                        equal(RealmHelper.getOverrideRealm(), "realm1", "returns override realm");
                    }));

                    test("#getOverrideRealm when realm override is present in query string and fragment query string", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentQueryString").returns("realm=realm1");
                        this.stub(URIUtils, "getCurrentFragmentQueryString").returns("realm=realm2");

                        equal(RealmHelper.getOverrideRealm(), "realm1", "returns query string realm");
                    }));

                    test("#getSubRealm when page is login", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentFragment").returns("login/realm1");

                        equal(RealmHelper.getSubRealm(), "realm1", "returns sub realm");
                    }));

                    test("#getSubRealm when page is not login and subRealm is already set", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentFragment").returns("other");
                        Configuration.globalData.auth.subRealm = "realm1";

                        equal(RealmHelper.getSubRealm(), "realm1", "returns sub realm");
                    }));

                    test("#getSubRealm when page is not login and subRealm is not set", sinon.test(function () {
                        this.stub(URIUtils, "getCurrentFragment").returns("other");
                        Configuration.globalData.auth.subRealm = "";

                        equal(RealmHelper.getSubRealm(), "", "returns empty string");
                    }));
                });
        }
    }
});