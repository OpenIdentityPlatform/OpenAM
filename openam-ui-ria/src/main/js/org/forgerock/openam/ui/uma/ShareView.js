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

/*global define, $, _ */

define("org/forgerock/openam/ui/uma/ShareView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/delegates/UmaDelegate",
    "org/forgerock/openam/ui/uma/util/UmaUtils",
    "org/forgerock/openam/ui/uma/models/User"
], function(AbstractView, conf, eventManager, uiUtils, constants, umaDelegate, umaUtils, User) {
    var ShareView = AbstractView.extend({
        template: "templates/uma/UmaInnerTemplate.html",
        events: {
          "click input#shareButton": "onShareButtonClicked"
        },

        render: function(args, callback) {
            var self = this,
                promise = umaUtils.getResourceSet(args[0], self.data.resourceSet);

            self.data.policyId = args[0];
            self.data.selected = {};

            $.when(promise).done(function(resourceSet){
                self.data.resourceSet = resourceSet;

                self.parentRender(function(){
                    self.$el.find('#selectUser select').selectize({
                        valueField: 'username',
                        labelField: 'username',
                        searchField: 'username',
                        create: false,
                        load: function(query, callback) {
                            if (query.length < self.MIN_QUERY_LENGTH) { return callback(); }

                            umaDelegate.searchUsers(query)
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
                            if (value === "") {
                                self.data.selected.user = null;
                            } else {
                              umaDelegate.getUser(value)
                              .done(function(data) {
                                  var user = new User(data.username);
                                      user.universalid = data.universalid[0];

                                  self.data.selected.user = user;
                              })
                              .fail(function() {
                                  self.$el.find("#selectUser select")[0].selectize.clear();
                                  self.enableShareButton();
                              });
                            }

                            self.enableShareButton();
                        }
                    });

                    self.$el.find('#selectPermission select').selectize({
                        plugins: ['restore_on_backspace'],
                        delimiter: ',',
                        persist: false,
                        create: false,
                        hideSelected: true,
                        onChange: function(value){
                            self.data.selected.permissions = value;
                            self.enableShareButton();
                        }
                    });
                });
            });
        },

        enableShareButton: function(){
            var complete = _.every(this.$el.find(".selectize-input"), function(element) {
                return $(element).hasClass("has-items");
            });
            this.$el.find('input#shareButton').prop('disabled', !complete);
        },

        onShareButtonClicked: function() {
          var self = this,
              permissions = [{
                  subject: this.data.selected.user.universalid,
                  scopes: this.data.selected.permissions
              }];

          umaDelegate.createPolicy(conf.loggedUser.userid.id, this.data.policyId, permissions).done(function(data, textStatus, xhr) {
              eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedSuccess");

              self.$el.find("#selectUser select")[0].selectize.clear();
              self.$el.find("#selectPermission select")[0].selectize.clear();
          }).fail(function(xhr, textStatus, errorThrown) {
              eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreatedFail");
          });
        }
    });

    return ShareView;
});
