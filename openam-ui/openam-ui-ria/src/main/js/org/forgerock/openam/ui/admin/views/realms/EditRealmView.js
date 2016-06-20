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
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/global/RealmsService",
    "org/forgerock/openam/ui/admin/services/global/AuthenticationService",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView",
    "org/forgerock/openam/ui/common/util/Promise",

    "bootstrap-tabdrop",
    "popoverclickaway",
    "selectize"
], ($, _, Handlebars, Messages, AbstractView, EventManager, Router, Constants, RealmsService,
    GlobalAuthenticationService, RealmAuthenticationService, FormHelper, Backlink, JSONSchema, JSONValues,
    FlatJSONSchemaView, Promise) => {

    function setAutofocus () {
        $("input[type=\"text\"]:not(:disabled):first").prop("autofocus", true);
    }

    function checkPattern (string) {
        // "Characters $, &, +, \, ", comma, /, :, ;, =, ?, @, space, #, %, <, > are not allowed in a realm's name."
        const specialChars = " @#$%&+?:;,/=\\<>\"";
        for (let i = 0; i < specialChars.length; i++) {
            if (string.indexOf(specialChars[i]) > -1) {
                return true;
            }
        }
        return false;
    }

    function isTopLevelRealm (name) {
        return name === "/";
    }

    function validateRealmName () {
        let valid = false;
        const realmName = $("input[name=\"root[name]\"]").val();
        let alert = "";

        if (realmName.length > 0) {
            if (checkPattern(realmName)) {
                alert = Handlebars.compile("{{> alerts/_Alert type='warning' " +
                                    "text='console.realms.realmNameValidationError'}}");
            } else {
                valid = true;
            }
        }

        $("[data-realm-alert]").html(alert);
        return valid;
    }

    const EditRealmView = AbstractView.extend({
        template: "templates/admin/views/realms/EditRealmTemplate.html",
        partials: [
            "partials/alerts/_Alert.html"
        ],
        events: {
            "click [data-save]": "handleSave",
            "click [data-cancel]": "returnBack",
            "click [data-delete]": "onDeleteClick",
            "keyup input[name=\"root[name]\"]": "onDataChange",
            "change input[name=\"root[name]\"]": "onDataChange"
        },

        render (args) {
            let realmPromise;
            let authenticationPromise;

            this.realmPath = args[0];

            if (args[0]) {
                this.data.realmPath = args[0];
                this.data.realmName = args[0] === "/" ? $.t("console.common.topLevelRealm") : args[0].split("/").pop();
                this.data.newEntity = false;
                this.data.headerActions = [{
                    actionPartial: "form/_Button",
                    data:"delete",
                    title:"common.form.delete",
                    icon:"fa-times",
                    disabled: !this.canRealmBeDeleted(this.data.realmPath)
                }];
            } else {
                this.data.newEntity = true;
                this.data.headerActions = null;
            }

            if (this.data.newEntity) {
                realmPromise = RealmsService.realms.schema();
                authenticationPromise = GlobalAuthenticationService.authentication.schema();
            } else {
                realmPromise = RealmsService.realms.get(this.data.realmPath);
                authenticationPromise = RealmAuthenticationService.authentication.get(this.data.realmPath);
            }

            this.parentRender(function () {
                Promise.all([realmPromise, RealmsService.realms.all(), authenticationPromise]).then((response) => {
                    const data = response[0];
                    const allRealmPaths = _.map(response[1][0].result, "path");

                    if (this.data.newEntity) {
                        // Only create dropdowns if the field is editable
                        data.schema.properties.parentPath["enum"] = allRealmPaths;
                        data.schema.properties.parentPath.options = { "enum_titles": allRealmPaths };
                        data.schema.properties.name.pattern = "^[^ @#$%&+?:;,/=\\<>\"]+$";
                    } else {
                        // Once created, it should not be possible to edit a realm's name or who it's parent is.
                        data.schema.properties.name.readonly = true;
                        data.schema.properties.parentPath.readonly = true;
                        if (isTopLevelRealm(data.values.name)) {
                            data.schema.properties.active.readonly = true;
                        }

                        this.toggleSubmitButton(true);
                    }

                    const generalPropertyPath = this.data.newEntity ? "schema.properties.defaults.general"
                                                                    : "schema.properties.general";

                    this.subviews = [
                        new FlatJSONSchemaView({
                            schema: new JSONSchema(data.schema),
                            values: new JSONValues(data.values)
                        }),
                        new FlatJSONSchemaView({
                            schema: new JSONSchema(_.get(response[2], generalPropertyPath))
                                        .addDefaultProperties(["statelessSessionsEnabled"]),
                            values: new JSONValues(_.get(response[2], "values.general"))
                        })
                    ];

                    this.subviews[0].render().$el.appendTo(this.$el.find("[data-realm-form]"));
                    // Turn off inline error messages as we display our own help on errors on this page
                    this.subviews[0].subview.jsonEditor.options["show_errors"] = "never";

                    this.subviews[1].render().$el.appendTo(this.$el.find("[data-realm-stateless-session-form]"));

                    new Backlink().render(0);
                    setAutofocus();
                }, (response) => {
                    Messages.addMessage({ type: Messages.TYPE_DANGER, response });
                });
            });
        },
        returnBack () {
            if (this.data.newEntity) {
                Router.routeTo(Router.configuration.routes.realms, { args: [], trigger: true });
            } else {
                Router.routeTo(Router.configuration.routes.realmDefault, {
                    args: [encodeURIComponent(this.data.realmPath)],
                    trigger: true
                });
            }
        },
        onDataChange () {
            this.toggleSubmitButton(validateRealmName());
        },
        handleSave (event) {
            event.preventDefault();
            this.toggleSubmitButton(false);

            const failCallback = (response) => { Messages.addMessage({ type: Messages.TYPE_DANGER, response }); };
            const values = this.subviews[0].getData();
            const statelessSessionsValues = this.subviews[1].getData();
            const savePromise = this.data.newEntity ? RealmsService.realms.create(values)
                                                    : RealmsService.realms.update(values);

            savePromise.then((realm) => {
                const isRootRealm = realm.name === "/";
                const hasRootRealmAsParent = realm.parentPath === "/";
                let realmPath;

                if (isRootRealm) {
                    realmPath = "/";
                } else if (hasRootRealmAsParent) {
                    realmPath = `/${realm.name}`;
                } else {
                    realmPath = `${realm.parentPath}/${realm.name}`;
                }

                RealmAuthenticationService.authentication.update(realmPath, statelessSessionsValues).then(() => {
                    if (this.data.newEntity) {
                        this.data.newEntity = false;
                        Router.routeTo(Router.configuration.routes.realmDefault, {
                            args: [encodeURIComponent(realmPath)],
                            trigger: true
                        });
                    } else {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    }

                }, failCallback);
            }, failCallback).always(() => {
                this.toggleSubmitButton(true);
            });
        },

        onDeleteClick (event) {
            event.preventDefault();
            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.realms.edit.realm") },
                _.bind(this.deleteRealm, this));
        },

        deleteRealm () {
            RealmsService.realms.remove(this.realmPath).then(() => {
                Router.routeTo(Router.configuration.routes.realms, { args: [], trigger: true });
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                if (response && response.status === 409) {
                    Messages.addMessage({
                        message: $.t("console.realms.parentRealmCannotDeleted"),
                        type: Messages.TYPE_DANGER
                    });
                } else {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                }
            });
        },

        canRealmBeDeleted (realmPath) {
            return realmPath === "/" ? false : true;
        },

        toggleSubmitButton (flag) {
            this.$el.find("[data-save]").prop("disabled", !flag);
        }
    });

    return new EditRealmView();
});
