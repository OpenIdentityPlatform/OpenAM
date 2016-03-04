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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "squire",
    "sinon"
], function (Squire, sinon) {
    describe("org/forgerock/openam/ui/admin/views/createRealmsBreadcrumbs", function () {
        let createRealmsBreadcrumbs, URIUtils, $;

        beforeEach(function (done) {
            var injector = new Squire();

            URIUtils = {
                getCurrentFragment: sinon.stub()
            };

            $ = {
                t: sinon.stub()
            };

            injector
                .mock("org/forgerock/commons/ui/common/util/URIUtils", URIUtils)
                .mock("jquery", $)
                .require(["org/forgerock/openam/ui/admin/views/realms/createRealmsBreadcrumbs"], function (obj) {
                    createRealmsBreadcrumbs = obj;
                    done();
                });
        });

        context("When on the Authentication - Dashboard view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/dashboard");
                $.t.returns("Authentication - Dashboard");
                expect(createRealmsBreadcrumbs()).to.eql([{
                    title:"Authentication - Dashboard"
                }]);
            }));
        });

        context("When on the Authentication - Settings view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-settings");
                $.t.returns("Authentication - Settings");
                expect(createRealmsBreadcrumbs()).to.eql([{
                    title:"Authentication - Settings"
                }]);
            }));
        });

        context("When on the Authentication - List Chains view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-chains");
                $.t.returns("Authentication - Chains");
                expect(createRealmsBreadcrumbs()).to.eql([{
                    title:"Authentication - Chains"
                }]);
            }));
        });

        context("When on the Authentication - New Chain view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-chains/new");
                $.t.returns("Authentication - Chains");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authentication - Chains",
                        path:"#realms/%2F/authentication-chains"
                    },
                    {
                        title:"new"
                    }
                ]);
            }));
        });

        context("When on the Authentication - Edit Chain view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-chains/edit/isthisthereallife");
                $.t.returns("Authentication - Chains");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authentication - Chains",
                        path:"#realms/%2F/authentication-chains"
                    },
                    {
                        title:"isthisthereallife"
                    }
                ]);
            }));
        });

        context("When on the Authentication - List Modules view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-modules");
                $.t.returns("Authentication - Modules");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authentication - Modules"
                    }
                ]);
            }));
        });

        context("When on the Authentication - New Module view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-modules/new");
                $.t.returns("Authentication - Modules");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authentication - Modules",
                        path:"#realms/%2F/authentication-modules"
                    },
                    {
                        title:"new"
                    }
                ]);
            }));
        });

        context("When on the Authentication - Edit Module view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-modules/isthis/edit/justfantasy");
                $.t.returns("Authentication - Modules");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authentication - Modules",
                        path:"#realms/%2F/authentication-modules"
                    },
                    {
                        title:"isthis"
                    },
                    {
                        title:"justfantasy"
                    }
                ]);
            }));
        });

        context("When on the List Services view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/services");
                $.t.returns("Services");
                expect(createRealmsBreadcrumbs()).to.eql([{
                    title:"Services"
                }]);
            }));
        });

        context("When on the New Service view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/new");
                $.t.returns("Services");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Services",
                        path:"#realms/%2F/services"
                    },
                    {
                        title:"new"
                    }
                ]);
            }));
        });

        context("When on the Edit Service view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/edit/audit");
                $.t.returns("Services");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Services",
                        path:"#realms/%2F/services"
                    },
                    {
                        title:"audit"
                    }
                ]);
            }));
        });

        context("When on the New Service Subschema view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/edit/audit/CSV/new");
                $.t.returns("Services");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Services",
                        path:"#realms/%2F/services"
                    },
                    {
                        title:"audit",
                        path:"#realms/%2F/services/edit/audit"
                    },
                    {
                        title:"CSV"
                    },
                    {
                        title:"new"
                    }
                ]);
            }));
        });

        context("When on the Edit Service Subschema view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/edit/audit/CSV/edit/caughtinalandslide");
                $.t.returns("Services");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Services",
                        path:"#realms/%2F/services"
                    },
                    {
                        title:"audit",
                        path:"#realms/%2F/services/edit/audit"
                    },
                    {
                        title:"CSV"
                    },
                    {
                        title:"caughtinalandslide"
                    }
                ]);
            }));
        });

        context("When on the List Policy Sets view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authorization-policySets");
                $.t.returns("Authorization - Policy Sets");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authorization - Policy Sets"
                    }
                ]);
            }));
        });

        context("When on the Edit Policy Set view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authorization-policySets/edit/noescape");
                $.t.returns("Authorization - Policy Sets");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authorization - Policy Sets",
                        path:"#realms/%2F/authorization-policySets"
                    },
                    {
                        title:"noescape"
                    }
                ]);
            }));
        });

        context("When on the New Policy view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns("realms/%2F/authorization-policySets/edit/noescape/policies/new");
                $.t.returns("Authorization - Policy Sets");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authorization - Policy Sets",
                        path:"#realms/%2F/authorization-policySets"
                    },
                    {
                        title:"noescape",
                        path:"#realms/%2F/authorization-policySets/edit/noescape"
                    },
                    {
                        title:"policies"
                    },
                    {
                        title:"new"
                    }
                ]);
            }));
        });

        context("When on the Edit Policy view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-policySets/edit/noescape/policies/edit/from%20reality");
                $.t.returns("Authorization - Policy Sets");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authorization - Policy Sets",
                        path:"#realms/%2F/authorization-policySets"
                    },
                    {
                        title:"noescape",
                        path:"#realms/%2F/authorization-policySets/edit/noescape"
                    },
                    {
                        title:"policies"
                    },
                    {
                        title:"from reality"
                    }
                ]);
            }));
        });

        context("When on the List Resource Types view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-resourceTypes");
                $.t.returns("Authorization - Resource Types");
                expect(createRealmsBreadcrumbs()).to.eql([{
                    title:"Authorization - Resource Types"
                }]);
            }));
        });

        context("When on the New Resource Type view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-resourceTypes/new");
                $.t.returns("Authorization - Resource Types");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authorization - Resource Types",
                        path:"#realms/%2F/authorization-resourceTypes"
                    },
                    {
                        title:"new"
                    }
                ]);
            }));
        });

        context("When on the Edit Resource Type view", function () {
            it("correctly outputs object breadcrumbs", sinon.test(function () {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-resourceTypes/edit/76656a38-5f8e-401b-83aa-4ccb74ce88d2");
                $.t.returns("Authorization - Resource Types");
                expect(createRealmsBreadcrumbs()).to.eql([
                    {
                        title:"Authorization - Resource Types",
                        path:"#realms/%2F/authorization-resourceTypes"
                    },
                    {
                        title:"76656a38-5f8e-401b-83aa-4ccb74ce88d2"
                    }
                ]);
            }));
        });

    });
});
