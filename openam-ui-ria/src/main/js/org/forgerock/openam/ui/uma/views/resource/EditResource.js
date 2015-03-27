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

/*global $, _, Backbone, define*/
define('org/forgerock/openam/ui/uma/views/resource/EditResource', [
    'org/forgerock/commons/ui/common/components/Messages',
    'org/forgerock/commons/ui/common/main/AbstractView',
    'backgrid',
    'org/forgerock/openam/ui/uma/util/BackgridUtils',
    'org/forgerock/commons/ui/common/util/Constants',
    'org/forgerock/commons/ui/common/main/EventManager',
    'org/forgerock/commons/ui/common/main/Router',
    'org/forgerock/commons/ui/common/util/UIUtils',
    'org/forgerock/openam/ui/uma/models/UMAResourceSetWithPolicy',
    'org/forgerock/openam/ui/uma/views/share/BootstrapModalShare',
    'bootstrap-dialog'
], function(Messages, AbstractView, Backgrid, BackgridUtils, Constants, EventManager, Router, UIUtils, UMAResourceSetWithPolicy, BootstrapModalShare, BootstrapDialog) {
    var EditResource = AbstractView.extend({
        initialize: function(options) {
            this.model = null;
        },
        template: "templates/uma/views/resource/EditResource.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            'click button#revokeAll': 'onRevokeAll',
            'click button#share': 'onShare'
        },
        onModelError: function(model, response) {
            console.error('Unrecoverable load failure UMAResourceSetWithPolicy. ' +
                response.responseJSON.code + ' (' + response.responseJSON.reason + ') ' +
                response.responseJSON.message);
        },
        onModelSync: function(model, response) {
            this.render();
        },
        onRevokeAll: function() {
            var self = this;
            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: self.model.get('name'),
                message: $.t("uma.resources.show.revokeAllMessage"),
                closable: false,
                buttons: [{
                    id: "btn-ok",
                    label: $.t("common.form.ok"),
                    cssClass: "btn-primary btn-danger",
                    action: function(dialog) {
                        dialog.enableButtons(false);
                        dialog.getButton("btn-ok").text($.t("common.form.working"));
                        self.model.get('policy').destroy().done(function (response) {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllPoliciesSuccess");
                            self.render();
                        }).fail(function (error) {
                            Messages.messages.addMessage({ message: JSON.parse(error.responseText).message, type: "error"});
                        }).always(function() {
                            dialog.close();
                        });
                    }
                }, {
                    label: $.t("common.form.cancel"),
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },
        onShare: function() {
            BootstrapModalShare.data.currentResourceSetId = this.model.id;
            BootstrapModalShare.render();
        },
        render: function(args, callback) {
            var collection, grid, id = null, options, paginator, RevokeCell, SelectizeCell, self = this;

            // Get the current id
            if(args && args[0]) { id = args[0]; }

            /**
             * Guard clause to check if model requires sync'ing/updating
             * Reason: We do not know the id of the data we need until the render function is called with args,
             * thus we can only check at this point if we have the correct model to render this view (the model
             * might already contain the correct data).
             * Behaviour: If the model does require sync'ing then we abort this render via the return and render
             * will it invoked again when the model is updated
             */
            if(this.syncModel(id)) { return; }

            /**
             * FIXME: Ideally the data needs to the be whole model, but I'm told it's also global so we're
             * copying in just the attributes I need ATM
             */
            this.data.name = this.model.get('name');

            // FIXME: Re-enable filtering and pagination
            //     UserPoliciesCollection = Backbone.PageableCollection.extend({
            //         url: URLHelper.substitute("__api__/users/__username__/oauth2/resourcesets/" + args[0]),
            //         parseRecords: function (data, options) {
            //             return data.policy.permissions;
            //         },
            //         sync: backgridUtils.sync
            //     });

            RevokeCell = BackgridUtils.TemplateCell.extend({
                template: "templates/uma/backgrid/cell/RevokeCell.html",
                events: {
                    "click #revoke": "revoke"
                },
                revoke: function(event) {
                    self.model.get('policy').get('permissions').remove(this.model);
                    self.model.get('policy').save();
                }
            });

            options = this.model.get('scopes').toJSON();

            SelectizeCell = Backgrid.Cell.extend({
                className: "selectize-cell",
                template: "templates/uma/backgrid/cell/SelectizeCell.html",
                render: function() {
                    var items = this.model.get('scopes').pluck('name'),
                        select;

                    this.$el.html(UIUtils.fillTemplateWithData(this.template));

                    select = this.$el.find('select').selectize({
                        create: false,
                        delimiter: ",",
                        dropdownParent: '#uma',
                        hideSelected: true,
                        persist: false,
                        labelField: 'name',
                        valueField: 'id',
                        items: items,
                        options: options
                    })[0];

                    select.selectize.lock();

                    /* This an extention of the original positionDropdown method within Selectize. The override is
                     * required because using the dropdownParent 'body' places the dropdown out of scope of the
                     * containing backbone view. However adding the dropdownParent as any other element, has problems
                     * due the offsets and/positioning being incorrecly calucaluted in orignal positionDropdown method.
                     */
                    select.selectize.positionDropdown = function() {
                        var $control = this.$control,
                            offset = this.settings.dropdownParent ? $control.offset() : $control.position();

                        if (this.settings.dropdownParent) {
                            offset.top  -= ($control.outerHeight(true)*2) + $(this.settings.dropdownParent).position().top;
                            offset.left -= $(this.settings.dropdownParent).offset().left + $(this.settings.dropdownParent).outerWidth() - $(this.settings.dropdownParent).outerWidth(true);
                        } else {
                            offset.top += $control.outerHeight(true);
                        }

                        this.$dropdown.css({
                            width : $control.outerWidth(),
                            top   : offset.top,
                            left  : offset.left
                        });
                    };

                    this.delegateEvents();
                    return this;
                }
            });

            /**
             * There *might* be no policy object present (if all the permissions were removed) so we're
             * checking for this and creating an empty collection if there is no policy
             */
            collection = new Backbone.Collection();
            if(this.model.has('policy')) { collection = this.model.get('policy').get('permissions'); }

            grid = new Backgrid.Grid({
                columns: [
                    {
                        name: "subject",
                        label: $.t("uma.resources.show.grid.0"),
                        cell: 'string',
                        // headerCell: BackgridUtils.FilterHeaderCell,
                        editable: false
                    },
                    {
                        name: "permissions",
                        label: $.t("uma.resources.show.grid.2"),
                        cell: SelectizeCell,
                        editable: false,
                        sortable: false
                    },
                    {
                        name: "edit",
                        label: "",
                        cell: RevokeCell,
                        editable: false,
                        sortable: false
                    }],
                collection: collection,
                emptyText: $.t("uma.all.grid.empty")
            });

            // FIXME: Re-enable filtering and pagination
            // paginator = new Backgrid.Extension.Paginator({
            //     collection: this.model.get('policy').get('permissions'),
            //     windowSize: 3
            // });



            this.parentRender(function() {
                self.$el.find('[data-toggle="tooltip"]').tooltip();
                if (self.model.has('policy') && self.model.get('policy').get('permissions').length > 0){
                    self.$el.find("button#revokeAll").prop("disabled", false);
                }
                self.$el.find("#backgridContainer").append(grid.render().el);
                // FIXME: Re-enable filtering and pagination
                // self.$el.find("#paginationContainer").append(paginator.render().el);
            });
        },
        syncModel: function(id) {
            var syncRequired = !this.model || (id && this.model.id !== id);

            if(syncRequired) {
                this.stopListening(this.model);
                this.model = UMAResourceSetWithPolicy.findOrCreate( { _id: id} );
                this.listenTo(this.model, 'sync', this.onModelSync);
                this.listenTo(this.model, 'error', this.onModelError);
                this.model.fetch();
            }

            return syncRequired;
        }
    });

    return new EditResource();
});
