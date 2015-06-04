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
            'click  button[data-active]'    : 'warningBeforeDeleteChain',
            'click  button[data-chain-name]:not([data-active])': 'deleteChain',
            'click  #deleteChains'          : 'deleteChains',
            'click  #addChain'              : 'addChain'
        },
        addChain: function(e) {
            e.preventDefault();
            var href = $(e.currentTarget).attr('href');
            BootstrapDialog.show({
                title: "Enter the chain name",
                message: '<p>Some helpful text here explaining that you need to name your chain before you can configure it</p><br/><input type="text" id="newName" class="form-control" placeholder="Enter Name"  value="">',
                buttons: [{
                    label: $.t("common.form.next"),
                    cssClass: "btn-primary",
                    action: function(dialog) {
                        dialog.close();
                        //TODO Check name first.
                        Router.navigate( href + dialog.getModalBody().find('#newName').val(), { trigger: true });
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
        warningBeforeDeleteChain: function(event) {
            var self = this,
                chainName = $(event.currentTarget).attr('data-chain-name');

            BootstrapDialog.show({
                title: "Delete " + chainName,
                type: BootstrapDialog.TYPE_DANGER,
                message: '<p>This chain is being used as one of the default chains. Deleting may result in locking out the administors or the users.</p><p>Are you sure you want to continue?</p>',
                buttons: [{
                    label: "Delete",
                    cssClass: "btn-danger",
                    action: function(dialog) {
                        self.deleteChain(event);
                    }
                }, {
                    label: "Cancel",
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },
        deleteChain: function(event) {
            var self = this,
                chainName = $(event.currentTarget).attr('data-chain-name');

            SMSDelegate.RealmAuthenticationChain.remove(chainName)
                .done(function(data) {
                    self.render();
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
                    self.render();
                })
                .fail(function() {
                    // TODO: Add failure condition
                });
        },
        editChain: function(e) {
            e.preventDefault();
            Router.navigate( '#console/chaining/', {trigger: true});
        },
        render: function (args, callback) {
            var self = this,
                sortedChains = [];
            this.data = {};

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
