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
], (Squire, sinon) => {
    describe("org/forgerock/openam/ui/admin/views/common/navigation/createBreadcrumbs", () => {
        let createBreadcrumbs;
        let URIUtils;
        let $;

        beforeEach((done) => {
            const injector = new Squire();

            URIUtils = {
                getCurrentFragment: sinon.stub()
            };

            $ = {
                t: sinon.stub()
            };

            injector
                .mock("org/forgerock/commons/ui/common/util/URIUtils", URIUtils)
                .mock("jquery", $)
                .require(["org/forgerock/openam/ui/admin/views/common/navigation/createBreadcrumbs"], (obj) => {
                    createBreadcrumbs = obj;
                    done();
                });
        });

        context("When on the Authentication - Dashboard view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/dashboard");
                $.t.returns("Authentication - Dashboard");
                const pattern = "realms/?/dashboard";
                expect(createBreadcrumbs(pattern)).to.eql([{
                    title:"Authentication - Dashboard"
                }]);
            }));
        });

        context("When on the Authentication - Settings view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-settings");
                $.t.returns("Authentication - Settings");
                const pattern = "realms/?/authentication-settings";
                expect(createBreadcrumbs(pattern)).to.eql([{
                    title:"Authentication - Settings"
                }]);
            }));
        });

        context("When on the Authentication - List Chains view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-chains");
                $.t.returns("Authentication - Chains");
                const pattern = "realms/?/authentication-chains";
                expect(createBreadcrumbs(pattern)).to.eql([{
                    title:"Authentication - Chains"
                }]);
            }));
        });

        context("When on the Authentication - New Chain view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-chains/new");
                $.t.returns("Authentication - Chains");
                const pattern = "realms/?/authentication-chains/new";
                expect(createBreadcrumbs(pattern)).to.eql([
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

        context("When on the Authentication - Edit Chain view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-chains/edit/foo");
                $.t.returns("Authentication - Chains");
                const pattern = "realms/?/authentication-chains/edit/?";
                expect(createBreadcrumbs(pattern)).to.eql([
                    {
                        title:"Authentication - Chains",
                        path:"#realms/%2F/authentication-chains"
                    },
                    {
                        title:"foo"
                    }
                ]);
            }));
        });

        context("When on the Authentication - List Modules view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-modules");
                $.t.returns("Authentication - Modules");
                const pattern = "realms/?/authentication-modules";
                expect(createBreadcrumbs(pattern)).to.eql([
                    {
                        title:"Authentication - Modules"
                    }
                ]);
            }));
        });

        context("When on the Authentication - New Module view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-modules/new");
                $.t.returns("Authentication - Modules");
                const pattern = "realms/?/authentication-modules/new";
                expect(createBreadcrumbs(pattern)).to.eql([
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

        context("When on the Authentication - Edit Module view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authentication-modules/foo/edit/bar");
                $.t.returns("Authentication - Modules");
                const pattern = "realms/?/authentication-modules/?/edit/?";
                expect(createBreadcrumbs(pattern)).to.eql([
                    {
                        title:"Authentication - Modules",
                        path:"#realms/%2F/authentication-modules"
                    },
                    {
                        title:"foo"
                    },
                    {
                        title:"bar"
                    }
                ]);
            }));
        });

        context("When on the List Services view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/services");
                $.t.returns("Services");
                const pattern = "realms/?/services";
                expect(createBreadcrumbs(pattern)).to.eql([{
                    title:"Services"
                }]);
            }));
        });

        context("When on the New Service view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/new");
                $.t.returns("Services");
                const pattern = "realms/?/services/new";
                expect(createBreadcrumbs(pattern)).to.eql([
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

        context("When on the Edit Service view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/edit/audit");
                $.t.returns("Services");
                const pattern = "realms/?/services/edit/?";
                expect(createBreadcrumbs(pattern)).to.eql([
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

        context("When on the New Service Subschema view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/edit/audit/CSV/new");
                $.t.returns("Services");
                const pattern = "realms/?/services/edit/?/?/new";
                expect(createBreadcrumbs(pattern)).to.eql([
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

        context("When on the Edit Service Subschema view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/services/edit/audit/CSV/edit/foo");
                $.t.returns("Services");
                const pattern = "realms/?/services/edit/?/?/edit/?";
                expect(createBreadcrumbs(pattern)).to.eql([
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
                        title:"foo"
                    }
                ]);
            }));
        });

        context("When on the List Policy Sets view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authorization-policySets");
                $.t.returns("Authorization - Policy Sets");
                const pattern = "realms/?/authorization-policySets";
                expect(createBreadcrumbs(pattern)).to.eql([
                    {
                        title:"Authorization - Policy Sets"
                    }
                ]);
            }));
        });

        context("When on the Edit Policy Set view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authorization-policySets/edit/foo");
                $.t.returns("Authorization - Policy Sets");
                const pattern = "realms/?/authorization-policySets/edit/?";
                expect(createBreadcrumbs(pattern)).to.eql([
                    {
                        title:"Authorization - Policy Sets",
                        path:"#realms/%2F/authorization-policySets"
                    },
                    {
                        title:"foo"
                    }
                ]);
            }));
        });

        context("When on the New Policy view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns("realms/%2F/authorization-policySets/edit/foo/policies/new");
                $.t.returns("Authorization - Policy Sets");
                const pattern = "realms/?/authorization-policySets/edit/?/policies/new";
                expect(createBreadcrumbs(pattern)).to.eql([
                    {
                        title:"Authorization - Policy Sets",
                        path:"#realms/%2F/authorization-policySets"
                    },
                    {
                        title:"foo",
                        path:"#realms/%2F/authorization-policySets/edit/foo"
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

        context("When on the Edit Policy view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-policySets/edit/foo/policies/edit/bar%20bar");
                $.t.returns("Authorization - Policy Sets");
                const pattern = "realms/?/authorization-policySets/edit/?/policies/edit/?";
                expect(createBreadcrumbs(pattern)).to.eql([
                    {
                        title:"Authorization - Policy Sets",
                        path:"#realms/%2F/authorization-policySets"
                    },
                    {
                        title:"foo",
                        path:"#realms/%2F/authorization-policySets/edit/foo"
                    },
                    {
                        title:"policies"
                    },
                    {
                        title:"bar bar"
                    }
                ]);
            }));
        });

        context("When on the List Resource Types view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-resourceTypes");
                $.t.returns("Authorization - Resource Types");
                const pattern = "realms/?/authorization-resourceTypes";
                expect(createBreadcrumbs(pattern)).to.eql([{
                    title:"Authorization - Resource Types"
                }]);
            }));
        });

        context("When on the New Resource Type view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-resourceTypes/new");
                $.t.returns("Authorization - Resource Types");
                const pattern = "realms/?/authorization-resourceTypes/new";
                expect(createBreadcrumbs(pattern)).to.eql([
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

        context("When on the Edit Resource Type view", () => {
            it("correctly outputs object breadcrumbs", sinon.test(() => {
                URIUtils.getCurrentFragment.returns(
                    "realms/%2F/authorization-resourceTypes/edit/76656a38-5f8e-401b-83aa-4ccb74ce88d2");
                $.t.returns("Authorization - Resource Types");
                const pattern = "realms/?/authorization-resourceTypes/edit/?";
                expect(createBreadcrumbs(pattern)).to.eql([
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
