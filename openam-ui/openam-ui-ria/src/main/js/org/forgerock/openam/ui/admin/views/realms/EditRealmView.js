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

define("org/forgerock/openam/ui/admin/views/realms/EditRealmView", [
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSGlobalService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/views/jsonSchema/JSONSchemaView",
    "org/forgerock/openam/ui/common/util/Promise",

    "bootstrap-tabdrop",
    "popoverclickaway",
    "selectize"
], function ($, _, Handlebars, Messages, AbstractView, EventManager, Router, Constants, SMSGlobalService,
             FormHelper, JSONSchema, JSONValues, JSONSchemaView, Promise) {

    function setAutofocus () {
        $("[data-realm-form] input[type=\"text\"]:not(:disabled):first").prop("autofocus", true);
    }

    function checkPattern (string) {
        // "Characters $, &, +, comma, /, :, ;, =, ?, @, space, #, %% are not allowed in a realm's name"
        const specialChars = " @#$%&+?:;,/=";
        for (let i = 0; i < specialChars.length; i++) {
            if (string.indexOf(specialChars[i]) > -1) {
                return true;
            }
        }
        return false;
    }

    function validateRealmName () {
        let valid = false;
        let realmName = _.trim($("[data-realm-form] input[name=\"root[name]\"]").val());
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
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html",
            "partials/alerts/_Alert.html",
            "partials/util/_HelpLink.html"
        ],
        events: {
            "click [data-save]": "submitForm",
            "click [data-cancel]": "returnBack",
            "click [data-delete]": "onDeleteClick",
            "keyup input[name=\"root[name]\"]": "onDataChange",
            "change input[name=\"root[name]\"]": "onDataChange"
        },

        render: function (args, callback) {
            let promise;
            let allRealmsPromise = SMSGlobalService.realms.all();

            if (args[0]) {
                this.data.realmPath = args[0];
                this.data.realmName = args[0] === "/" ? $.t("console.common.topLevelRealm") : args[0].split("/").pop();
                this.data.deleteBtnDisabled = !this.canRealmBeDeleted(this.data.realmPath);
                this.data.newEntity = false;
            } else {
                this.data.newEntity = true;
            }

            if (this.data.newEntity) {
                promise = SMSGlobalService.realms.schema();
            } else {
                promise = SMSGlobalService.realms.get(this.data.realmPath);
            }

            this.parentRender(function () {
                Promise.all([promise, allRealmsPromise]).then((results) => {
                    let data = results[0];
                    let element = this.$el.find("[data-realm-form]");
                    let allRealmPaths = [];

                    _.each(results[1][0].result, (realm) => {
                        if (realm.path) {
                            allRealmPaths.push(realm.path);
                        }
                    });

                    if (this.data.newEntity) {
                        // Only create dropdowns if the field is editable
                        data.schema.properties.parentPath["enum"] = allRealmPaths;
                        data.schema.properties.parentPath.options = { "enum_titles": allRealmPaths };
                        data.schema.properties.name.pattern = "^[^$&+,/:;=?@\ #%]+$";
                    } else {
                        // Once created, it should not be possible to edit a realm's name or who it's parent is.
                        data.schema.properties.name.readonly = true;
                        data.schema.properties.parentPath.readonly = true;

                        this.toggleSubmitButton(true);
                    }

                    if (this.jsonSchemaView) {
                        this.jsonSchemaView.remove();
                    }

                    this.jsonSchemaView = new JSONSchemaView({
                        values: new JSONValues(data.values),
                        schema: new JSONSchema(data.schema)
                    });
                    $(this.jsonSchemaView.render().el).appendTo(element);

                    setAutofocus();

                    if (callback) {
                        callback();
                    }
                }, (response) => {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        response: response
                    });
                });
            });
        },

        returnBack: function () {
            if (this.data.newEntity) {
                Router.routeTo(Router.configuration.routes.realms, {
                    args: [],
                    trigger: true
                });
            } else {
                Router.routeTo(Router.configuration.routes.realmDefault, {
                    args: [encodeURIComponent(this.data.realmPath)],
                    trigger: true
                });
            }
        },

        onDataChange: function () {
            if (this.jsonSchemaView.views.length && this.jsonSchemaView.views[0].jsonEditor) {
                this.jsonSchemaView.views[0].jsonEditor.options["show_errors"] = "never";
            }
            this.toggleSubmitButton(validateRealmName());
        },

        submitForm: function (event) {
            event.preventDefault();
            this.toggleSubmitButton(false);

            let promise = this.data.newEntity ? SMSGlobalService.realms.create(this.jsonSchemaView.values())
                            : SMSGlobalService.realms.update(this.jsonSchemaView.values());

            promise.then((realm) => {
                if (this.data.newEntity) {
                    this.data.newEntity = false;
                    let realmPath = realm.parentPath === "/" ? `/${realm.name}` : `${realm.parentPath}/${realm.name}`;
                    Router.routeTo(Router.configuration.routes.realmDefault, {
                        args: [encodeURIComponent(realmPath)],
                        trigger: true
                    });
                } else {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                }
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: response
                });
            }).always(() => {
                this.toggleSubmitButton(true);
            });
        },

        onDeleteClick: function (event) {
            event.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.realms.edit.realm") },
                _.bind(this.deleteRealm, this));
        },

        deleteRealm: function () {
            let realmPath = this.jsonSchemaView.values().name;

            SMSGlobalService.realms.remove(realmPath).then(() => {
                Router.routeTo(Router.configuration.routes.realms, {
                    args: [],
                    trigger: true
                });
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                if (response && response.status === 409) {
                    Messages.addMessage({
                        message: $.t("console.realms.parentRealmCannotDeleted"),
                        type: Messages.TYPE_DANGER
                    });
                } else {
                    Messages.addMessage({
                        response: response,
                        type: Messages.TYPE_DANGER
                    });
                }
            });
        },

        canRealmBeDeleted: function (realmPath) {
            return realmPath === "/" ? false : true;
        },

        toggleSubmitButton: function (flag) {
            this.$el.find("[data-save]").prop("disabled", !flag);
        }
    });

    return new EditRealmView();
});
