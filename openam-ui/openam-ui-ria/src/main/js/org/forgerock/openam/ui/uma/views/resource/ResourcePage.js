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

/*global define*/
define("org/forgerock/openam/ui/uma/views/resource/ResourcePage", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backbone",
    "backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "bootstrap-dialog",
    "org/forgerock/openam/ui/uma/views/share/CommonShare",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/uma/models/UMAResourceSetWithPolicy"
], function ($, _, AbstractView, Backbone, Backgrid, BackgridUtils, BootstrapDialog, CommonShare, Constants, EventManager,
            Messages, Router, UIUtils, UMAResourceSetWithPolicy) {
    var ResourcePage = AbstractView.extend({
        initialize: function () {
            // TODO: AbstarctView.prototype.initialize.call(this);
            this.model = null;
        },
        template: "templates/uma/views/resource/ResourceTemplate.html",
        events: {
            "click button#starred": "onStarred",
            "click button#share": "onShare",
            "click li#unshare": "onUnshare",
            "click button#editLabels": "editLabels",
            "click button#saveLabels": "submitLabelsChanges",
            "click button#disgardLabels": "disgardLabelsChanges"
        },
        onModelError: function (model, response) {
            console.error("Unrecoverable load failure UMAResourceSetWithPolicy. " +
                response.responseJSON.code + " (" + response.responseJSON.reason + ") " +
                response.responseJSON.message);
        },
        onModelSync: function () {
            this.render();
        },
        onUnshare: function (event) {
            event.preventDefault();

            if ($(event.currentTarget).hasClass("disabled")) { return; }

            var self = this;

            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: self.model.get("name"),
                message: $.t("uma.resources.show.revokeAllMessage"),
                closable: false,
                buttons: [{
                    id: "btnOk",
                    label: $.t("common.form.ok"),
                    cssClass: "btn-primary btn-danger",
                    action: function (dialog) {
                        dialog.enableButtons(false);
                        dialog.getButton("btnOk").text($.t("common.form.working"));
                        self.model.get("policy").destroy().done(function () {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllPoliciesSuccess");
                            self.render();
                        }).fail(function (error) {
                            Messages.messages.addMessage({ message: JSON.parse(error.responseText).message, type: "error"});
                        }).always(function () {
                            dialog.close();
                        });
                    }
                }, {
                    label: $.t("common.form.cancel"),
                    action: function (dialog) {
                        dialog.close();
                    }
                }]
            });
        },
        onShare: function() {
            var shareView = new CommonShare();
            shareView.renderDialog(this.model.id);
        },
        onStarred: function () {
            // TODO: Simply flips the icon ATM, model update and save still TODO
            // this.model.toggleStarred();
            // self.model.save().done(function() {
            this.$el.find("#starred i").toggleClass("fa-star-o fa-star");
            // });
        },
        renderLabelsOptions: function () {
            var self = this,
                labelsSelect = this.$el.find("#labels").selectize({
                    plugins: ["restore_on_backspace"],
                    delimiter: ",",
                    persist: false,
                    create: true,
                    hideSelected: true,
                    items: self.data.resourceSetLabels,
                    onChange: function () {
                        self.$el.find("button#saveLabels").prop("disabled", false);
                    },
                    render: {
                        item: function (item) {
                            return "<div data-value=\"" + item.value + "\" class=\"item\">" + item.value + "</div>\"";
                        }
                    }
                })[0];
            labelsSelect.selectize.lock();
            self.$el.find(".labels-container .btn-group").hide();

        },

        render: function (args, callback) {
            var collection, grid, id = _.last(args), options, RevokeCell, SelectizeCell, self = this;
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
            this.data = {};
            this.data.name = this.model.get("name");
            this.data.icon = this.model.get("icon_uri");
            //TODO: This data should come the server
            this.data.resourceSetLabels = ["label1", "label2", "label3"];
            this.data.allLabels = ["label1", "label2", "label3", "label4", "label5", "label6"];

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
                revoke: function() {
                    self.model.get("policy").get("permissions").remove(this.model);
                    self.model.get("policy").save();
                }
            });

            options = this.model.get("scopes").toJSON();

            SelectizeCell = Backgrid.Cell.extend({
                className: "selectize-cell",
                template: "templates/uma/backgrid/cell/SelectizeCell.html",
                render: function() {
                    var items = this.model.get("scopes").pluck("name"),
                        select;

                    this.$el.html(UIUtils.fillTemplateWithData(this.template));

                    select = this.$el.find("select").selectize({
                        create: false,
                        delimiter: ",",
                        dropdownParent: "#uma",
                        hideSelected: true,
                        persist: false,
                        labelField: "name",
                        valueField: "id",
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
                            offset.top -= ($control.outerHeight(true) * 2) + $(this.settings.dropdownParent).position().top;
                            offset.left -= $(this.settings.dropdownParent).offset().left + $(this.settings.dropdownParent).outerWidth() - $(this.settings.dropdownParent).outerWidth(true);
                        } else {
                            offset.top += $control.outerHeight(true);
                        }

                        this.$dropdown.css({
                            width: $control.outerWidth(),
                            top: offset.top,
                            left: offset.left
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
            if (this.model.has("policy")) {
                collection = this.model.get("policy").get("permissions");
            }

            grid = new Backgrid.Grid({
                columns: [
                    {
                        name: "subject",
                        label: $.t("uma.resources.show.grid.0"),
                        cell: "string",
                        editable: false,
                        headerCell: BackgridUtils.ClassHeaderCell.extend({
                            className: "col-md-4"
                        })
                    },
                    {
                        name: "permissions",
                        label: $.t("uma.resources.show.grid.2"),
                        cell: SelectizeCell,
                        editable: false,
                        sortable: false,
                        headerCell: BackgridUtils.ClassHeaderCell.extend({
                            className: "col-md-6"
                        })
                    },
                    {
                        name: "edit",
                        label: "",
                        cell: RevokeCell,
                        editable: false,
                        sortable: false,
                        headerCell: BackgridUtils.ClassHeaderCell.extend({
                            className: "col-md-2"
                        })
                    }],
                collection: collection,
                emptyText: $.t("console.common.noResults"),
                className: "backgrid table table-striped"
            });

            // FIXME: Re-enable filtering and pagination
            // paginator = new Backgrid.Extension.Paginator({
            //     collection: this.model.get('policy').get('permissions'),
            //     windowSize: 3
            // });

            this.parentRender(function() {
                self.$el.find("[data-toggle=\"tooltip\"]").tooltip();
                self.renderLabelsOptions();

                if (self.model.has("policy") && self.model.get("policy").get("permissions").length > 0){
                    self.$el.find("li#unshare").removeClass("disabled");
                }

                self.$el.find("#backgridContainer").append(grid.render().el);
                // FIXME: Re-enable filtering and pagination
                // self.$el.find("#paginationContainer").append(paginator.render().el);

                self.$el.find("#umaShareImage img").error(function () {
                    $(this).parent().addClass("no-image");
                });

                // TODO: To be decided off the labels passed by the server
                self.$el.find("#star i").toggleClass("fa-star-o fa-star");

                if(callback) { callback(); }
            });
        },
        deactivateLabels: function () {
            this.$el.find(".labels-container .btn-group").hide();
            this.$el.find("#editLabels").show();
            this.$el.find("#labels")[0].selectize.lock();
            this.$el.find(".labels-container .selectize-control ").addClass("pull-left");
        },
        editLabels: function () {
            var labelsSelect = this.$el.find("#labels")[0];
            this.data.labelsCopy = _.clone(labelsSelect.selectize.getValue());
            labelsSelect.selectize.unlock();
            labelsSelect.selectize.focus();
            this.$el.find("#editLabels").hide();
            this.$el.find(".labels-container .selectize-control ").removeClass("pull-left");
            this.$el.find("button#saveLabels").prop("disabled", true);
            this.$el.find(".labels-container .btn-group").show();

        },

        submitLabelsChanges: function () {
            var selectedValues = this.$el.find("#labels")[0].selectize.getValue();
            this.deactivateLabels();
            //TODO: update the model
        },
        disgardLabelsChanges: function () {
            var labelsSelect = this.$el.find("#labels")[0];
            this.deactivateLabels();
            labelsSelect.selectize.clear();
            _.each(this.data.labelsCopy, function (val) {
                labelsSelect.selectize.addItem(val);
            });

        },
        syncModel: function(id) {
            var syncRequired = !this.model || (id && this.model.id !== id);

            if(syncRequired) {
                this.stopListening(this.model);
                this.model = UMAResourceSetWithPolicy.findOrCreate( { _id: id} );
                this.listenTo(this.model, "sync", this.onModelSync);
                this.listenTo(this.model, "error", this.onModelError);
                this.model.fetch();
            }

            return syncRequired;
        }
    });

    return ResourcePage;
});
