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
    'org/forgerock/openam/ui/uma/models/UMAPolicy',
    'org/forgerock/openam/ui/uma/models/UMAPolicyPermission',
    'org/forgerock/openam/ui/uma/models/UMAResourceSetWithPolicy',
    "org/forgerock/openam/ui/uma/models/User"
], function(AbstractView, Constants, EventManager, UMADelegate, UMAPolicy, UMAPolicyPermission, UMAResourceSetWithPolicy, User) {
    var CommonShare = AbstractView.extend({
        initialize: function(options) {
            this.model = null;
            this.parentModel = null;
        },
        template: "templates/uma/views/share/CommonShare.html",
        events: {
          "click input#shareButton": "save",
          "click #toggleAdvanced": "onToggleAdvanced"
        },
        onParentModelError: function(model, response) {
            console.error('Unrecoverable load failure UMAResourceSetWithPolicy. ' +
                           response.responseJSON.code + ' (' + response.responseJSON.reason + ') ' +
                           response.responseJSON.message);
        },
        onParentModelSync: function(model, response) {
            // Create new UMA Policy object if one does not exist
            if(!model.has('policy')) {
                model.set('policy', new UMAPolicy());
                model.get('policy').createRequired = true;
            }

            // Hardwire the policyID into the policy as it's ID
            model.get('policy').set('policyId', this.parentModel.id);

            this.render();
        },

        /**
         * @returns Boolean whether the parent model required sync'ing
         */
        syncParentModel: function(id) {
            var syncRequired = !this.parentModel || (id && this.parentModel.id !== id);

            if(syncRequired) {
                this.stopListening(this.parentModel);

                this.parentModel = UMAResourceSetWithPolicy.findOrCreate( { _id: id} );

                this.listenTo(this.parentModel, 'sync', this.onParentModelSync);
                this.listenTo(this.parentModel, 'error', this.onParentModelError);

                this.parentModel.fetch();
            }

            return syncRequired;
        },

        render: function(args, callback) {
            var self = this;

            // FIXME: Resolve unknown issue with args appearing as an Array
            if(args instanceof Array) {
                this.data.dialog = args[1]; // This needs a tidyup. I am passing in true as the second args[1] to indicate the parent is a dialog
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
            if (this.syncParentModel(args)) { return; }

            this.data.name = this.parentModel.get('name');
            this.data.scopes = this.parentModel.get('scopes').toJSON();
            this.data.shareCount = self.getShareCount(); // returns null (not 0) if empty.
            this.data.shareInfo = self.getShareInfo(this.data.shareCount);

            this.parentRender(function() {
                self.renderUserOptions();
                self.renderPermissionOptions();
            });
        },

        getShareCount: function() {
            var count,
                policy = this.parentModel.get('policy');
            if (policy && policy.get('permissions')){
                count =  policy.get('permissions').length;
            }
            return count;
        },

        getShareInfo: function(count) {
            var shareInfo = $.t("uma.share.info", { context : "none" } );
            if (count){
                shareInfo =  $.t("uma.share.info", { count: count });
            }
            return shareInfo;
        },

        renderPermissionOptions: function() {
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

                    if(self.model) { self.model.set('scopes', values); }
                }
            });
        },
        renderUserOptions: function() {
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
                    /**
                     * Instantly set the value on the model. This is so that when there is an empty value
                     * the change events are fired properly
                     */
                    if(self.model) { self.model.set('subject', value); }

                    if(!value) { return; }

                    UMADelegate.getUser(value)
                    .done(function(data) {
                        var universalID = data.universalid[0],
                            existing = self.parentModel.get('policy').get("permissions").findWhere({ subject: universalID }),
                            model = existing ? existing : UMAPolicyPermission.findOrCreate( { subject: universalID } );

                        self.setModel(model);
                    });
                }
            });
        },
        setModel: function(value) {
            this.stopListening(this.model);

            this.model = value;

            if(value) {
                this.listenTo(this.model, 'change', this.onModelChange);
            }
        },
        onModelChange: function(model) {
            this.$el.find("#selectPermission select")[0].selectize.setValue(model.get("scopes"));
            this.$el.find('input#shareButton').prop('disabled', !model.isValid());
        },
        reset: function() {
            this.setModel(null);

            this.$el.find("#selectUser select")[0].selectize.clear();
            this.$el.find("#selectPermission select")[0].selectize.clear();
            this.$el.find('input#shareButton').prop('disabled', true);

            /**
             * Just update the message, not the icon, because we're always adding shares and thus
             * we're not going to be going back to the 'no shares' state
             */
            this.$el.find("span#shareInfo")[0].innerHTML = this.getShareInfo(this.getShareCount());
        },
        save: function() {
            var self = this,
                permissions = this.parentModel.get('policy').get("permissions"),
                existing = permissions.findWhere({ subject: this.model.get('subject') });

            if(!existing) {
                permissions.add(this.model);
            }

            this.parentModel.get('policy').save()
            .done(function(response) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedSuccess");

                self.parentModel.get('policy').createRequired = false;

                self.reset();
            })
            .fail(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedFail");
            });
        },

        onToggleAdvanced: function(e) {
            e.preventDefault();
            console.log('onToggleAdvanced');
        }
    });

    return CommonShare;
});
