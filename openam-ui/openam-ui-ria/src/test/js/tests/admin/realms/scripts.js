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

/*global define, QUnit */

define("tests/admin/realms/scripts", [
    "jquery",
    "sinon",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/util/UIUtils",
], function ($, sinon, BootstrapDialog, EventManager, Configuration, Router, Constants, ModuleLoader, UIUtils) {
    return {
        executeAll: function () {
            QUnit.module("Admin Scripts Tests");

            QUnit.moduleStart(function () {
                Configuration.loggedUser = {
                    "userName": "amadmin",
                    "roles": ["ui-admin"]
                };
            });

            QUnit.asyncTest("Scripts List", function () {
                ModuleLoader.load("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView").then(function (RealmTreeNavigationView) {

                    RealmTreeNavigationView.element = "<div></div>";
                    $("#qunit-fixture").append(RealmTreeNavigationView.$el);

                    sinon.stub(RealmTreeNavigationView, "render", function (args) {
                        RealmTreeNavigationView.render.restore();

                        RealmTreeNavigationView.data.realmPath = args[0];
                        RealmTreeNavigationView.data.realmName = this.data.realmPath === "/" ?
                            $.t("console.common.topLevelRealm") : this.data.realmPath;

                        RealmTreeNavigationView.parentRender(function () {
                            ModuleLoader.load("org/forgerock/openam/ui/admin/views/realms/scripts/ScriptsView").then(function (ScriptsView) {
                                var page = new ScriptsView();
                                page.element = RealmTreeNavigationView.$el.find("#sidePageContent");

                                page.render(["/"], function () {
                                    equal(page.$el.find(".backgrid").length, 1, "Backgrid renders");
                                    start();
                                });
                                RealmTreeNavigationView.delegateEvents();
                            });
                        });
                    });

                    Router.routeTo(Router.configuration.routes.realmsScripts, {
                        args: [encodeURIComponent("/")],
                        trigger: true
                    });
                });
            });

            QUnit.asyncTest("Create Script", function () {
                ModuleLoader.load("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView").then(function (RealmTreeNavigationView) {

                    RealmTreeNavigationView.element = "<div></div>";
                    $("#qunit-fixture").append(RealmTreeNavigationView.$el);

                    sinon.stub(RealmTreeNavigationView, "render", function (args) {
                        RealmTreeNavigationView.render.restore();

                        RealmTreeNavigationView.data.realmPath = args[0];
                        RealmTreeNavigationView.data.realmName = this.data.realmPath === "/" ?
                            $.t("console.common.topLevelRealm") : this.data.realmPath;

                        RealmTreeNavigationView.parentRender(function () {
                            ModuleLoader.load("org/forgerock/openam/ui/admin/views/realms/scripts/EditScriptView").then(function (EditScriptView) {
                                var page = new EditScriptView();
                                page.element = RealmTreeNavigationView.$el.find("#sidePageContent");

                                sinon.stub(page, "renderDialog", function () {
                                    BootstrapDialog.show(_.extend(this.constructDialogOptions(), {
                                            onshow: function (dialog) {
                                                var self = this;

                                                UIUtils.fillTemplateWithData("templates/admin/views/realms/scripts/ChangeContextTemplate.html",
                                                    page.data, function (tpl) {
                                                        self.message.append(tpl);
                                                        page.data.newScript = false;

                                                        var scriptType = dialog.$modalContent.find("[name=changeContext]:checked").val(),
                                                            scriptTypeName = dialog.$modalContent.find("[name=changeContext]:checked").parent().text().trim();
                                                        ok(scriptType, "Default script type is selected");

                                                        dialog.$modalContent.find("button.btn-primary").trigger("click");
                                                        setTimeout(function () {
                                                            equal(page.$el.find("#context").html(), scriptTypeName, "Script type is changed");
                                                            ok(page.$el.find("[name=language]:checked").length !== 0, "Language is selected");
                                                            ok(page.$el.find("#script").val().trim() !== "", "Script text is present");
                                                            equal(page.$el.find("#script").css("display"), "none", "Script textarea is hidden");
                                                            ok(page.$el.find(".codemirror").html() !== "", "Codemirror text area has rendered");
                                                            page.$el.find("#validateScript").trigger("click");
                                                            setTimeout(function () {
                                                                ok(page.$el.find("#validation .media-body").html().trim(),
                                                                    "No errors found", "No script errors found");
                                                                start();
                                                            }, 500);
                                                        }, 500);
                                                    });
                                            }
                                        }
                                    ));
                                });
                                page.render(["/"]);

                                RealmTreeNavigationView.delegateEvents();
                            });
                        });
                    });

                    Router.routeTo(Router.configuration.routes.realmsScriptEdit, {
                        args: [encodeURIComponent("/")],
                        trigger: true
                    });
                });
            });

            QUnit.asyncTest("Edit Script", function () {
                ModuleLoader.load("org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView").then(function (RealmTreeNavigationView) {
                    RealmTreeNavigationView.element = "<div></div>";
                    $("#qunit-fixture").append(RealmTreeNavigationView.$el);

                    sinon.stub(RealmTreeNavigationView, "render", function (args) {
                        RealmTreeNavigationView.render.restore();

                        RealmTreeNavigationView.data.realmPath = args[0];
                        RealmTreeNavigationView.data.realmName = this.data.realmPath === "/" ?
                            $.t("console.common.topLevelRealm") : this.data.realmPath;


                        RealmTreeNavigationView.parentRender(function () {
                            ModuleLoader.load("org/forgerock/openam/ui/admin/views/realms/scripts/EditScriptView").then(function (EditScriptView) {

                                var page = new EditScriptView();
                                page.element = RealmTreeNavigationView.$el.find("#sidePageContent");

                                page.render(["/", "9de3eb62-f131-4fac-a294-7bd170fd4acb"], function () {
                                    var entity = page.data.entity;
                                    ok(page.$el.find("#name").val() === entity.name, "Name is set");
                                    equal(page.$el.find("#script").val(), entity.script, "Script text is present");
                                    equal(page.$el.find("input[name=language]:checked").val(), entity.language, "Language is set");
                                    equal(page.$el.find("#context").html(), _.findWhere(page.data.contexts,function (context) {
                                        return context._id === entity.context;
                                    }).name, "Script type is set");
                                    start();
                                });
                                RealmTreeNavigationView.delegateEvents();
                            });
                        });
                    });

                    Router.routeTo(Router.configuration.routes.realmsScriptEdit, {
                        args: [encodeURIComponent("/"), "9de3eb62-f131-4fac-a294-7bd170fd4acb"],
                        trigger: true
                    });
                });
            });
        }
    }
});