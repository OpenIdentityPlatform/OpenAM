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
    "org/forgerock/openam/ui/uma/delegates/UMADelegate",
    "org/forgerock/openam/ui/uma/models/UMAResourceSetWithPolicy",

    // jquery dependencies
    "selectize"
], function ($, _, AbstractView, Backbone, Backgrid, BackgridUtils, BootstrapDialog, CommonShare, Constants, EventManager,
             Messages, Router, UIUtils, UMADelegate, UMAResourceSetWithPolicy) {
    function filterUserLabels(labels) {
        return _.filter(labels, function(label) {
            return label.type === "USER";
        });
    }
    function getAllUserLabels() {
        return UMADelegate.labels.all().then(function(labels) {
            return filterUserLabels(labels.result);
        });
    }
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
            "click .page-toolbar .selectize-input.disabled": "editLabels",
            "click button#saveLabels": "submitLabelsChanges",
            "click button#discardLabels": "discardLabelsChanges"
        },
        onModelError: function (model, response) {
            console.error("Unrecoverable load failure UMAResourceSetWithPolicy. " +
                response.responseJSON.code + " (" + response.responseJSON.reason + ") " +
                response.responseJSON.message);
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
                            Messages.addMessage({ response: error.responseText, type: Messages.TYPE_DANGER });
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
                labelsSelect = this.$el.find("#labels select").selectize({
                    plugins: ["restore_on_backspace"],
                    delimiter: ",",
                    persist: false,
                    create: true,
                    hideSelected: true,
                    onChange: function () {
                        self.$el.find("button#saveLabels").prop("disabled", false);
                    },
                    render: {
                        item: function (item) {
                            return "<div data-value=\"" + item.name + "\" class=\"item\">" + item.name + "</div>\"";
                        }
                    },
                    labelField: "name",
                    valueField: "name",
                    searchField: ["name"]
                })[0];
            labelsSelect.selectize.disable();
            this.updateLabelOptions();
            this.$el.find(".page-toolbar .btn-group").hide();
        },
        updateLabelOptions: function () {
            var labelsSelectize = this.$el.find("#labels select")[0].selectize;
            labelsSelectize.clearOptions();
            labelsSelectize.addOption(this.allLabels);
            labelsSelectize.clear();
            _.each(this.labels, function (item) {
                labelsSelectize.addItem(item);
            });
        },
        render: function (args, callback) {
            var id = _.last(args), self = this;

            if (this.model && this.id === id) {
                if (!this.isCurrentlyFetchingData) {
                    this.renderWithModel(callback);
                }
            } else {
                this.isCurrentlyFetchingData = true;
                $.when(
                    getAllUserLabels(),
                    UMAResourceSetWithPolicy.findOrCreate({_id: id}).fetch()
                ).done(function(allLabels, model) {
                    // Ensure we don't render any previous requests that were cancelled.
                    if (model.id === self.id) {
                        self.allLabels = allLabels;
                        self.model = model;
                        self.labels = _(self.model.get("labels")).map(function(labelId) {
                            return _.find(self.allLabels, function(otherLabel) {
                                return otherLabel._id === labelId;
                            });
                        }).compact().sortBy("name").pluck("name").value();
                        self.isCurrentlyFetchingData = false;
                        self.renderWithModel(callback);
                    }
                }).fail(this.onModelError);
            }
            this.id = id;
        },
        renderWithModel: function (callback) {
            var collection, grid, options, RevokeCell, SelectizeCell, self = this;

            /**
             * FIXME: Ideally the data needs to the be whole model, but I'm told it's also global so we're
             * copying in just the attributes I need ATM
             */
            this.data = {};
            this.data.name = this.model.get("name");
            this.data.icon = this.model.get("icon_uri");

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

                    select.selectize.disable();

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
                        sortable: false,
                        headerCell: BackgridUtils.ClassHeaderCell.extend({
                            className: "col-btn"
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
            this.$el.find(".page-toolbar .btn-group").hide();
            this.$el.find("#editLabels").show();
            this.$el.find("#labels select")[0].selectize.disable();
            this.$el.find("#labels .selectize-control").addClass("pull-left");
        },
        editLabels: function () {
            var labelsSelect = this.$el.find("#labels select")[0];
            this.data.labelsCopy = _.clone(labelsSelect.selectize.getValue());
            labelsSelect.selectize.enable();
            labelsSelect.selectize.focus();
            this.$el.find("#editLabels").hide();
            this.$el.find("#labels .selectize-control").removeClass("pull-left");
            this.$el.find("button#saveLabels").prop("disabled", true);
            this.$el.find("button#discardLabels").prop("disabled", false);
            this.$el.find(".page-toolbar .btn-group").show();

        },
        submitLabelsChanges: function () {
            var self = this,
                labelsSelectize = this.$el.find("#labels select")[0].selectize,
                selectedLabelNames = labelsSelectize.getValue(),
                allLabelNames = _.pluck(this.allLabels, "name"),
                newLabelNames = _.difference(selectedLabelNames, allLabelNames),
                existingLabelNames = _.intersection(selectedLabelNames, allLabelNames),
                existingLabelIds = _.map(existingLabelNames, function(labelName) {
                    return _.find(self.allLabels, function(otherLabel) {
                        return otherLabel.name === labelName;
                    })._id;
                }),
                creationPromises = _.map(newLabelNames, function(labelName) {
                    return UMADelegate.labels.create(labelName, "USER");
                });

            labelsSelectize.disable();
            self.$el.find("button#saveLabels").prop("disabled", true);
            self.$el.find("button#discardLabels").prop("disabled", true);
            $.when.apply($, creationPromises).then(function() {
                var newIds;
                if (creationPromises.length === 1) {
                    newIds = [arguments[0]._id];
                } else {
                    newIds = _.map(arguments, function (arg) {
                        return arg[0]._id;
                    });
                }
                self.model.set("labels", existingLabelIds.concat(newIds));
                return $.when(self.model.save(), getAllUserLabels());
            }).then(function(saveResult, allLabels) {
                self.labels = selectedLabelNames;
                self.allLabels = allLabels;
                self.updateLabelOptions();
                labelsSelectize.enable();
                self.deactivateLabels();
            }, function() {
                labelsSelectize.enable();
                self.$el.find("button#saveLabels").prop("disabled", false);
                self.$el.find("button#discardLabels").prop("disabled", false);
            });
        },
        discardLabelsChanges: function () {
            var labelsSelect = this.$el.find("#labels select")[0];
            this.deactivateLabels();
            labelsSelect.selectize.clear();
            _.each(this.labels, function (val) {
                labelsSelect.selectize.addItem(val);
            });
        }
    });
    return ResourcePage;
});
