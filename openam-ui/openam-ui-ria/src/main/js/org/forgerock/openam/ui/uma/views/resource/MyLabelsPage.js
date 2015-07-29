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
define("org/forgerock/openam/ui/uma/views/resource/MyLabelsPage", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backbone",
    "backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "bootstrap-dialog",
    "org/forgerock/openam/ui/uma/views/share/CommonShare",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/uma/views/resource/MyResourcesTab",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/uma/views/resource/SharedResourcesTab",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate"
], function($, AbstractView, Backbone, Backgrid, BackgridUtils, BootstrapDialog, CommonShare, Configuration, Constants, EventManager, Messages, MyResourcesTab, RealmHelper, SharedResourcesTab, UMADelegate) {
    var MyLabelsPage = AbstractView.extend({
        template: "templates/uma/views/resource/MyLabelsPageTemplate.html",
        partials: [
            "templates/uma/views/resource/_LabelListButtons.html"
        ],
        events: {
            "click button#deleteBtn": "deleteBtn"
        },
        deleteBtn: function() {
            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: $.t("uma.labels.list.deleteResourceLabel"),
                message: $.t("uma.labels.list.deleteLabelMessage"),
                buttons: [{
                    id: "btnOk",
                    label: $.t("common.form.ok"),
                    cssClass: "btn-primary btn-danger",
                    action: function(dialog) {
                        dialog.enableButtons(false);
                        dialog.getButton("btnOk").text($.t("common.form.working"));
                        // TODO - Delete Label
                        /*UMADelegate.revokeAllResources().done(function() {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllResourcesSuccess");
                        }).fail(function(error) {
                            Messages.messages.addMessage({ message: JSON.parse(error.responseText).message, type: "error"});
                        }).always(function() {
                            dialog.close();
                        });*/
                    }
                }, {
                    label: $.t("common.form.cancel"),
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },

        render: function(args, callback) {

            var self = this,
                labelId = args[0],
                columns,
                grid,
                paginator,
                ResourceSetCollection;

            UMADelegate.labels.get(labelId, "user").done( function(label){

                if(!label){
                    self.parentRender(function() {
                        if (callback) {
                            callback();
                        }
                    });
                } else {

                    self.data.label = label;

                    ResourceSetCollection = Backbone.PageableCollection.extend({
                        url: RealmHelper.decorateURIWithRealm("/" + Constants.context + "/json/__subrealm__/users/" + Configuration.loggedUser.username + "/oauth2/resourcesets"),
                        queryParams:  BackgridUtils.getQueryParams({
                            _sortKeys: BackgridUtils.sortKeys,
                            _queryFilter: ['resourceOwnerId eq "' + Configuration.loggedUser.username + '"'],
                            _pagedResultsOffset: BackgridUtils.pagedResultsOffset
                        }),
                        state: BackgridUtils.getState(),
                        parseState: BackgridUtils.parseState,
                        parseRecords: function(data, options){
                            self.$el.find("button#deleteBtn").prop("disabled", data.result.length === 0);
                            return data.result;
                        },
                        sync: function (method, model, options) {
                           options.beforeSend = function (xhr) {
                               xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
                           };
                           return BackgridUtils.sync(method, model, options);
                        }
                    });

                    columns = [
                        {
                            name: "name",
                            label: $.t("uma.resources.list.myResources.grid.0"),
                            cell: BackgridUtils.UriExtCell,
                            headerCell: BackgridUtils.FilterHeaderCell,
                            href: function(rawValue, formattedValue, model){
                                return "#uma/resources/mylabels/" + self.data.label._id + "/" + model.get("_id");
                            },
                            editable: false
                        },
                        {
                            name: "resourceServer",
                            label: $.t("uma.resources.list.myResources.grid.1"),
                            cell: "string",
                            editable: false
                        },
                        {
                            name: "type",
                            label: $.t("uma.resources.list.myResources.grid.2"),
                            cell: "string",
                            editable: false
                        },
                        {
                            name: "share",
                            label: "",
                            cell: Backgrid.Cell.extend({
                                className: "fa fa-share col-btn",
                                events: { "click": "share" },
                                share: function(e) {
                                    var shareView = new CommonShare();
                                    shareView.renderDialog(this.model.get("_id"));
                                },
                                render: function () {
                                    this.$el.attr({"title": $.t("uma.share.shareResource")});
                                    this.delegateEvents();
                                    return this;
                                }
                            }),
                            editable: false,
                            headerCell : BackgridUtils.ClassHeaderCell.extend({
                                className: "col-btn"
                            })
                        }
                    ];

                    self.data.resourceSetCollection = new ResourceSetCollection();
                    self.data.resourceSetCollection.on("backgrid:sort", BackgridUtils.doubleSortFix);

                    grid = new Backgrid.Grid({
                        columns: columns,
                        className:"backgrid table table-striped",
                        collection: self.data.resourceSetCollection,
                        emptyText: $.t("console.common.noResults")
                    });

                    paginator = new Backgrid.Extension.Paginator({
                        collection: self.data.resourceSetCollection,
                        windowSize: 3
                    });

                    self.parentRender(function() {
                        self.$el.find(".backgrid-container").append(grid.render().el);
                        self.$el.find(".pagination-container").append(paginator.render().el);
                        self.data.resourceSetCollection.fetch({reset: true, processData: false}).done(function () {
                            if (callback) {
                                callback();
                            }
                        });
                    });
                }

            });

        }
    });

    return MyLabelsPage;
});
