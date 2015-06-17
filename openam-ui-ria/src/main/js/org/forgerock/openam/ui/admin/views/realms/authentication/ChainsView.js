/*
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

/*global, define*/
define("org/forgerock/openam/ui/admin/views/realms/authentication/ChainsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate"
], function ($, _, AbstractView, BootstrapDialog, Router, SMSDelegate) {
    var ChainsView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/ChainsTemplate.html",
        events: {
            'change input[data-chain-name]' : 'chainSelected',
            'click  button.delete-chain-btn': 'deleteChain',
            'click  #deleteChains'          : 'deleteChains',
            'click  #selectAll'             : 'selectAll',
            'click  #addChain'              : 'addChain'
        },
        addChain: function(e) {
            e.preventDefault();
            var self = this,
                chainName,
                invalidName = false,
                href = $(e.currentTarget).attr('href');

            BootstrapDialog.show({
                title: "Enter the chain name",
                message: '<p>Some helpful text here explaining that you need to name your chain before you can configure it</p><br/><input type="text" id="newName" class="form-control" placeholder="Enter Name"  value="">',
                buttons: [{
                    label: $.t("common.form.next"),
                    cssClass: "btn-primary",
                    action: function(dialog) {
                        chainName = dialog.getModalBody().find('#newName').val();

                        invalidName = _.find(self.data.sortedChains, function(chain){
                            return chain._id === chainName;
                        });

                        if (invalidName){
                            // TODO - duplicate chain name - message or alert box
                            console.log('invalidName'); // jslinter
                        } else {

                            SMSDelegate.RealmAuthenticationChains.create({_id : chainName})
                            .done(function(data) {
                                dialog.close();
                                Router.navigate( href + dialog.getModalBody().find('#newName').val(), { trigger: true });
                            })
                            .fail(function() {
                                // TODO: Add failure condition
                            });
                        }
                    }
                },{
                    label: $.t("common.form.cancel"),
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },
        chainSelected: function(event) {
            var hasChainsSelected = this.$el.find('input[type=checkbox]').is(':checked'),
                row = $(event.currentTarget).closest('tr'),
                checked = $(event.currentTarget).is(':checked');

            this.$el.find('#deleteChains').prop('disabled', !hasChainsSelected);
            if (checked) {
                row.addClass('selected');
            } else {
                row.removeClass('selected');
            }
        },
        selectAll: function(event) {
            var checked = $(event.currentTarget).is(':checked');
            this.$el.find('.sorted-chains input[type=checkbox]:not(:disabled)').prop('checked', checked);
            if (checked) {
                this.$el.find('.sorted-chains').addClass('selected');
            } else {
                this.$el.find('.sorted-chains').removeClass('selected');
            }
            this.$el.find('#deleteChains').prop('disabled', !checked);
        },
        deleteChain: function(event) {
            var self = this,
                chainName = $(event.currentTarget).attr('data-chain-name');

            SMSDelegate.RealmAuthenticationChain.remove(chainName)
                .done(function(data) {
                    self.render([self.data.realmLocation]);
                })
                .fail(function() {
                    // TODO: Add failure condition
                });
        },
        deleteChains: function() {
            var self = this,
                chainNames = self.$el.find('input[type=checkbox]:checked').toArray().map(function(element) {
                    return $(element).attr('data-chain-name');
                }),
                promises = chainNames.map(function(name) {
                    return SMSDelegate.RealmAuthenticationChain.remove(name);
                });

            $.when(promises)
                .done(function(data) {
                    self.render([self.data.realmLocation]);
                })
                .fail(function() {
                    // TODO: Add failure condition
                });
        },
        render: function (args, callback) {
            var self = this,
                sortedChains = [];
            this.data.realmLocation = args[0];

            SMSDelegate.RealmAuthenticationChains.getWithDefaults()
                .done(function(data) {
                    _.each(data.values.result, function(obj) {
                        // Add default chains to top of list.
                        if ( obj.active) {
                            sortedChains.unshift(obj);
                        } else {
                            sortedChains.push(obj);
                        }
                    });
                    self.data.sortedChains = sortedChains;
                    self.parentRender(function() {
                        if (callback) {
                            callback();
                        }
                    });
                })
                .fail(function() {
                    // TODO: Add failure condition
                });
        }
    });

    return ChainsView;
});
