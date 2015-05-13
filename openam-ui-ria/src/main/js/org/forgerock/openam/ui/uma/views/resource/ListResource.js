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

/*global define, $, _, Backgrid, Backbone*/

define("org/forgerock/openam/ui/uma/views/resource/ListResource", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backgrid",
    "org/forgerock/openam/ui/uma/util/BackgridUtils",
    "org/forgerock/commons/ui/common/components/BSDialog",
    "org/forgerock/openam/ui/uma/views/share/CommonShare",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate"
], function(AbstractView, Backgrid, BackgridUtils, BSDialog, CommonShare, Configuration, Constants, EventManager, MessageManager, RealmHelper, Router, UMADelegate) {

    var ListResource = AbstractView.extend({
        template: "templates/uma/views/resource/ListResource.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            'click button#revokeAll:not(.disabled)': 'onRevokeAll'
        },
        onRevokeAll: function() {
            var revokeDialog = new BSDialog();
            revokeDialog.setTitle($.t("uma.resources.show.revokeAll"));
            revokeDialog.closable = false;
            revokeDialog.type = "type-danger";
            revokeDialog.message = $.t("uma.resources.show.revokeAllResourcesMessage");
            revokeDialog.actions = [{
                id: "btnOk",
                label: $.t("common.form.ok"),
                cssClass: "btn-primary btn-danger",
                action: function(dialog) {
                    dialog.enableButtons(false);
                    dialog.getButton("btnOk").text($.t("common.form.working"));
                    UMADelegate.revokeAllResources().done(function() {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllResourcesSuccess");
                    }).fail(function(error) {
                        MessageManager.messages.addMessage({ message: JSON.parse(error.responseText).message, type: "error"});
                    }).always(function() {
                        dialog.close();
                    });
                }
            },{
                type: "close"
            }];
            revokeDialog.show();
        },

        render: function(args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                ResourceSetCollection;

            ResourceSetCollection = Backbone.PageableCollection.extend({
                url: RealmHelper.decorateURIWithRealm("/" + Constants.context + "/json/__subrealm__/users/" + Configuration.loggedUser.username + '/oauth2/resourcesets'),
                state: {
                    pageSize: 10,
                    sortKey: "name"
                },
                queryParams: {
                    pageSize: "_pageSize",
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryId: "*",
                    _queryFilter: BackgridUtils.queryFilter,
                    _pagedResultsOffset:  BackgridUtils.pagedResultsOffset,
                    _fields: ['_id', 'icon_uri', 'name', 'resourceServer', 'type']
                },

                parseState: BackgridUtils.parseState,
                parseRecords: function(data, options){
                    self.$el.find("button#revokeAll").attr("disabled", data.result.length === 0);

                    return data.result;
                },
                sync: BackgridUtils.sync
            });

            columns = [
                {
                    name: "name",
                    label: $.t("uma.resources.list.grid.0"),
                    cell: BackgridUtils.UriExtCell,
                    headerCell: BackgridUtils.FilterHeaderCell.extend({
                        addClassName: "col-md-5"
                    }),
                    href: function(rawValue, formattedValue, model){
                        return "#uma/resources/" + model.get('_id');
                    },
                    editable: false
                },
                {
                    name: "resourceServer",
                    label: $.t("uma.resources.list.grid.1"),
                    /*cell: BackgridUtils.UriExtCell,
                    cheaderCell: BackgridUtils.FilterHeaderCell,
                    href: function(rawValue, formattedValue, model){
                        return "#uma/apps/" + encodeURIComponent(model.get('resourceServer'));
                    },*/
                    cell: "string",
                    editable: false,
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-1"
                    })
                },
                {
                    name: "type",
                    label: $.t("uma.resources.list.grid.2"),
                    cell: "string",
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-4"
                    }),
                    editable: false
                },
                {
                    name: "share",
                    label: "",
                    cell: Backgrid.Cell.extend({
                        className: "fa fa-share-alt",
                        events: { "click": "share" },
                        share: function(e) {
                            var shareView = new CommonShare();
                            shareView.renderDialog(this.model.get('_id'));
                        },
                        render: function () {
                            this.$el.attr({"title": $.t("uma.share.shareResource")});
                            this.delegateEvents();
                            return this;
                        }
                    }),
                    editable: false,
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-1"
                    })
                }
            ];

            self.data.resourceSetCollection = new ResourceSetCollection();
            self.data.resourceSetCollection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns: columns,
                className:"backgrid table table-striped",
                collection: self.data.resourceSetCollection,
                emptyText: $.t("uma.all.grid.empty")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.resourceSetCollection,
                windowSize: 3
            });

            self.parentRender(function() {
                self.$el.find('[data-toggle="tooltip"]').tooltip();
                self.$el.find("#backgridContainer").append( grid.render().el );
                self.$el.find("#paginationContainer").append( paginator.render().el );
                self.data.resourceSetCollection.fetch({reset: true, processData: false});
                if (callback) { callback();}
            });
        }

    });

    return new ListResource();
});
