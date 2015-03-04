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

/*global $, _, define*/
define("org/forgerock/openam/ui/uma/views/share/CommonShare", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/uma/delegates/UmaDelegate",
    "org/forgerock/openam/ui/uma/models/UMAPolicy",
    "org/forgerock/openam/ui/uma/models/UMAPolicyPermission",
    "org/forgerock/openam/ui/uma/models/UMAPolicyPermissionCollection",
    "org/forgerock/openam/ui/uma/models/UMAResourceSet",
    "org/forgerock/openam/ui/uma/models/User"
], function(AbstractView, Constants, EventManager, UMADelegate, UMAPolicy, UMAPolicyPermission, UMAPolicyPermissionCollection, UMAResourceSet, User) {
    var CommonShare = AbstractView.extend({
        template: "templates/uma/views/share/CommonShare.html",
        events: {
          "click input#shareButton": "save"
        },

        load: function(id) {
            var self = this,
                policyPromise,
                resourceSetPromise;

            // UMA Policy
            this.data.policy = new UMAPolicy({
                policyId: id
            });
            policyPromise = this.data.policy.fetch();
            policyPromise.fail(function() {
                this.data.policy.fakeCreate = true;
            });

            // UMA Resource Set
            this.data.resourceSet = new UMAResourceSet({
                _id: id
            });
            resourceSetPromise = this.data.resourceSet.fetch();
            resourceSetPromise
            .done(function() {
                self.data.notFound = false;
            })
            .fail(function() {
                self.data.notFound = true;
            });

            this.data.policyPermission = new UMAPolicyPermission();
            this.listenTo(this.data.policyPermission, 'change', this.onPolicyPermissionChanged);

            // TODO: Should also be catching failure (umaPolicy fails if the policy is new)
            $.when(policyPromise, resourceSetPromise)
            .always(function() {
                self.parentRender(function() {
                    self.renderUserSelect();
                    self.renderPermissionSelect();
                    self.renderInfo();
                });
            });
        },

        onPolicyPermissionChanged: function(model, options) {
            this.$el.find("#selectPermission select")[0].selectize.setValue(model.get("scopes"));
            this.$el.find('input#shareButton').prop('disabled', !model.isValid());
        },

        render: function(currentResourceSetId, callback) {
            this.load(currentResourceSetId);

            if(callback) { callback(); }
        },

        renderInfo: function() {
            var text = $.t("uma.share.info", { context: "none" });
            if(this.data.policy.get("permissions").length) {
                text = $.t("uma.share.info", { count: this.data.policy.get("permissions").length });
            }
            this.$el.find("#shareCounter > p").text(text);
        },

        renderPermissionSelect: function() {
            var self = this;

            this.$el.find('#selectPermission select').selectize({
                plugins: ['restore_on_backspace'],
                delimiter: ',',
                persist: false,
                create: false,
                hideSelected: true,
                onChange: function(values) {
                    // TODO: This is not ideal, reset from defaults?
                    values = values || [];
                    self.data.policyPermission.set("scopes", values);
                }
            });
        },

        renderUserSelect: function() {
            var self = this;

            this.$el.find('#selectUser select').selectize({
                valueField: 'username',
                labelField: 'username',
                searchField: 'username',
                create: false,
                load: function(query, callback) {
                    if (query.length < self.MIN_QUERY_LENGTH) { return callback(); }

                    UMADelegate.searchUsers(query)
                    .then(function(data) {
                        return _.map(data.result, function(username) {
                            return new User(username);
                        });
                    })
                    .done(function(users) {
                        callback(users);
                    })
                    .fail(function(event){
                        console.error('error', event);
                        callback();
                    });
                },
                onChange: function(value) {
                    self.data.policyPermission.set("subject", value);

                    if(value) {
                        UMADelegate.getUser(value)
                        .done(function(data) {
                            var universalID = data.universalid[0],
                                existingPolicyPermission = self.data.policy.get("permissions").findWhere({subject: universalID});

                            self.data.policyPermission.set("subject", universalID);
                            if(existingPolicyPermission) {
                                self.data.policyPermission.set("scopes", existingPolicyPermission.get("scopes"));
                            }
                        });
                    }
                }
            });
        },

        reset: function() {
            this.data.policyPermission.clear().set(this.data.policyPermission.defaults);

            this.$el.find("#selectUser select")[0].selectize.clear();
            this.$el.find("#selectPermission select")[0].selectize.clear();
            this.$el.find('input#shareButton').prop('disabled', true);

            this.renderInfo();
        },

        save: function() {
            var self = this,
                existing = this.data.policy.get("permissions").findWhere({subject: self.data.policyPermission.get("subject")});

            if(existing) {
                existing.set(self.data.policyPermission.attributes);
            } else {
                this.data.policy.get("permissions").add(self.data.policyPermission);
            }

            this.data.policy.save()
            .done(function(response) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedSuccess");

                self.reset();
            })
            .fail(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedFail");
            });
        }
    });

    return CommonShare;
});
