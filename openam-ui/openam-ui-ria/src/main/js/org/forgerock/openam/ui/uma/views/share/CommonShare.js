/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

define("org/forgerock/openam/ui/uma/views/share/CommonShare", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/uma/views/share/ShareCounter",
    "org/forgerock/openam/ui/uma/models/UMAPolicy",
    "org/forgerock/openam/ui/uma/models/UMAPolicyPermission",
    "org/forgerock/openam/ui/uma/models/UMAPolicyPermissionScope",
    "org/forgerock/openam/ui/uma/models/UMAResourceSetWithPolicy",
    "org/forgerock/openam/ui/common/util/RealmHelper",

    // jquery dependencies
    "selectize"
], function ($, _, AbstractView, Backgrid, BackgridUtils, BootstrapDialog, Constants, EventManager, ShareCounter,
             UMAPolicy, UMAPolicyPermission, UMAPolicyPermissionScope, UMAResourceSetWithPolicy, RealmHelper) {

    var CommonShare,
        realmRegex = /[?&]realm=/,
        resourcesetsRegex = /\/json\/users\/[^\/]+\/uma\/policies/,
        policyRegex = /\/json\/users\/[^\/]+\/oauth2\/resourcesets/;

    $.ajaxPrefilter(function (options) {
        if ((resourcesetsRegex.test(options.url) || policyRegex.test(options.url)) && !realmRegex.test(options.url) &&
                RealmHelper.getOverrideRealm()) {
            options.url = RealmHelper.decorateURLWithOverrideRealm(options.url);
        }
    });

    CommonShare = AbstractView.extend({
        initialize: function () {
            this.parentModel = null;
        },
        template: "templates/uma/views/share/CommonShare.html",
        events: {
            "click input#shareButton": "save"
        },
        enableOrDisableShareButton: function () {
            var subjectValid = this.$el.find("#selectUser select")[0].selectize.getValue().length > 0,
                permissionsValid = this.$el.find("#selectPermission select")[0].selectize.getValue().length > 0;

            this.$el.find("input#shareButton").prop("disabled", !(subjectValid && permissionsValid));
        },
        onParentModelError: function (model, response) {
            console.error("Unrecoverable load failure UMAResourceSetWithPolicy. " +
                           response.responseJSON.code + " (" + response.responseJSON.reason + ") " +
                           response.responseJSON.message);
            // TODO : Fire and event message
        },
        onParentModelSync: function (model) {
            // Hardwire the policyID into the policy as it's ID
            model.get("policy").set("policyId", this.parentModel.id);

            this.render();
        },

        /*
         * @returns Boolean whether the parent model required sync'ing
         */
        syncParentModel: function (id) {
            var syncRequired = !this.parentModel || (id && this.parentModel.id !== id);

            if (syncRequired) {
                this.stopListening(this.parentModel);

                this.parentModel = UMAResourceSetWithPolicy.findOrCreate({ _id: id });
                this.listenTo(this.parentModel, "sync", this.onParentModelSync);
                this.listenTo(this.parentModel, "error", this.onParentModelError);
                this.parentModel.fetch();
            }

            return syncRequired;
        },

        renderDialog: function (args, callback) {
            var self = this,
                $div = $("<div></div>"),
                options = {
                    type: BootstrapDialog.TYPE_PRIMARY,
                    title: $.t("uma.share.shareResource"),
                    size: BootstrapDialog.SIZE_WIDE,
                    cssClass: "shareDialog",
                    message: $div,
                    buttons: [{
                        label: $.t("common.form.close"),
                        cssClass: "btn-default",
                        action: function (dialog) {
                            dialog.close();
                        }
                    }],
                    onshow: function () {
                        self.element = $div;
                        self.render(args, callback);
                    },

                    onshown: function () {
                        self.renderShareCounter(callback);
                    }
                };

            BootstrapDialog.show(options);
        },

        render: function (args, callback) {
            var self = this,
                collection,
                grid;

            // FIXME: Resolve unknown issue with args appearing as an Array
            if (args instanceof Array) {
                args = args[0];
            }

            /**
             * Guard clause to check if model requires sync'ing/updating
             * Reason: We do not know the id of the data we need until the render function is called with args,
             * thus we can only check at this point if we have the correct model to render this view (the model
             * might already contain the correct data).
             * Behaviour: If the model does require sync'ing then we abort this render via the return and render
             * will it invoked again when the model is updated
             */
            if (this.syncParentModel(args)) {
                return;
            }
            this.data = {};
            this.data.name = this.parentModel.get("name");
            this.data.icon = this.parentModel.get("icon_uri");
            this.data.scopes = this.parentModel.get("scopes").toJSON();
            collection = this.parentModel.get("policy").get("permissions");
            this.data.permissions = collection.toJSON();

            grid = new Backgrid.Grid({
                columns: [{
                    name: "subject",
                    label: $.t("uma.resources.show.grid.0"),
                    cell: "string",
                    editable: false,
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-6"
                    })
                },
                {
                    name: "scopes",
                    label: $.t("uma.resources.show.grid.2"),
                    cell: Backgrid.Cell.extend({
                        render: function () {
                            var formatted = this.model.get("scopes").pluck("name").join(", ");
                            this.$el.empty();
                            this.$el.append(formatted);
                            this.delegateEvents();
                            return this;
                        }

                    }),
                    editable: false,
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-6"
                    })
                }],
                collection: collection,
                emptyText: $.t("console.common.noResults"),
                className:"backgrid table"
            });

            this.parentRender(function () {
                self.renderUserOptions();
                self.renderPermissionOptions();
                self.renderShareCounter(callback);
                self.$el.find("#advancedView").append(grid.render().el);
                self.$el.find("#umaShareImage img").error(function () {
                    $(this).parent().addClass("fa-file-image-o no-image");
                });
            });
        },
        renderPermissionOptions: function () {
            var self = this;

            this.$el.find("#selectPermission select").selectize({
                plugins: ["restore_on_backspace"],
                delimiter: ",",
                persist: false,
                create: false,
                hideSelected: true,
                onChange: function () {
                    self.enableOrDisableShareButton();
                }
            });
        },
        renderUserOptions: function () {
            var self = this;

            this.$el.find("#selectUser select").selectize({
                addPrecedence: true,
                create: true, // TODO: false when search for users is enabled
                // TODO: Disable looking up users
                // load: function (query, callback) {
                //     if (query.length < self.MIN_QUERY_LENGTH) {
                //         return callback();
                //     }
                //
                //     UMADelegate.searchUsers(query)
                //     .then(function (data) {
                //         return _.map(data.result, function (username) {
                //             return new User(username);
                //         });
                //     })
                //     .done(function (users) {
                //         callback(users);
                //     })
                //     .fail(function (event) {
                //         console.error('error', event);
                //         callback();
                //     });
                // },
                onChange: function (value) {
                    // Look for existing share and populate permissions if there one already exists

                    var existing = self.parentModel.get("policy").get("permissions").findWhere({ subject: value }),
                        scopes,
                        selectPermission = self.$el.find("#selectPermission select");

                    if (existing) {
                        scopes = existing.get("scopes").pluck("name");
                        selectPermission[0].selectize.focus();
                        selectPermission[0].selectize.setValue(scopes);
                    }

                    self.enableOrDisableShareButton();
                }
            });
        },
        reset: function () {
            this.$el.find("#selectUser select")[0].selectize.clear();
            this.$el.find("#selectPermission select")[0].selectize.clear();
            this.$el.find("input#shareButton").prop("disabled", true);

            this.renderShareCounter();
        },
        save: function () {
            var self = this,
                permissions = this.parentModel.get("policy").get("permissions"),
                subjects = this.$el.find("#selectUser select")[0].selectize.getValue(),
                scopes = _.each(this.$el.find("#selectPermission select")[0].selectize.getValue(), function (scope) {
                    return UMAPolicyPermissionScope.find({ id: scope });
                }),
                newPermissions = [];
            _.forEach(subjects, function (subject) {
                var permission = UMAPolicyPermission.findOrCreate({
                    subject: subject,
                    scopes: scopes
                });
                permissions.add(permission);
                newPermissions.push(permission);
            });

            this.parentModel.get("policy").save()
            .done(function () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedSuccess");
                self.reset();
            })
            .fail(function (response) {
                if (response.status && response.status === 500) {
                    _.forEach(newPermissions, function (permission) {
                        permissions.remove(permission);
                    });
                }
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedFail");
            });
        },

        renderShareCounter: function (callback) {
            var policy = this.parentModel.get("policy"),
                permissionCount = policy ? policy.get("permissions").length : 0;
            ShareCounter.render(permissionCount, callback);
        }
    });

    return CommonShare;
});
