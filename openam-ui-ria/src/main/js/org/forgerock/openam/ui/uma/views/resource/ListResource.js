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
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate",
    "org/forgerock/openam/ui/uma/util/BackgridUtils",
    "org/forgerock/openam/ui/uma/util/UMAUtils",
    "org/forgerock/openam/ui/uma/views/resource/DialogRevokeAllResources",
    "backgrid"
], function(MessageManager, AbstractView, Configuration, EventManager, Router, Constants, UMADelegate, BackgridUtils, UMAUtils, DialogRevokeAllResources, Backgrid) {

    var ListResource = AbstractView.extend({
        template: "templates/uma/views/resource/ListResource.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",

        events: {
            'click a#revokeAll:not(.disabled)': 'onRevokeAll'
        },
        onRevokeAll: function() {

            var confirmCallBack = function(){
                UMADelegate.revokeAllResources().done(function(){
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllResourcesSuccess");
                    }).fail(function(error){
                        MessageManager.messages.addMessage({ message: JSON.parse(error.responseText).message, type: "error"});
                    });
                };

            DialogRevokeAllResources.render(confirmCallBack);

        },

        render: function(args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                ResourceSetCollection,
                realm = UMAUtils.getRealm();

            ResourceSetCollection = Backbone.PageableCollection.extend({
                url: "/" + Constants.context + "/json" + realm + "/users/" + Configuration.loggedUser.username + '/oauth2/resourcesets',
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
                    if(data.result.length === 0){
                        self.$el.find("a#revokeAll").addClass("disabled");
                    } else {
                        self.$el.find("a#revokeAll").removeClass("disabled");
                    }
                    return data.result;
                },
                sync: BackgridUtils.sync
            });

            columns = [
                {
                    name: "share",
                    label: "",
                    cell: Backgrid.Cell.extend({
                        className: "icon-share",
                        events: { "click": "share" },
                        share: function(e) {
                            self.data.currentResourceSetId = this.model.get('_id');

                            EventManager.sendEvent(Constants.EVENT_SHOW_DIALOG,{
                                route: Router.configuration.routes.dialogShare,
                                noViewChange: true
                            });
                        },
                        render: function () {
                            this.delegateEvents();
                            return this;
                        }
                    }),
                    editable: false
                },
                {
                    name: "name",
                    label: $.t("uma.resources.list.grid.0"),
                    cell: BackgridUtils.UriExtCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    href: function(rawValue, formattedValue, model){
                        return "#uma/resources/" + model.get('_id');
                    },
                    editable: false
                },
                {
                    name: "resourceServer",
                    label: $.t("uma.resources.list.grid.1"),
                    cell: BackgridUtils.UriExtCell,
                    /*headerCell: BackgridUtils.FilterHeaderCell,
                    href: function(rawValue, formattedValue, model){
                        return "#uma/apps/" + encodeURIComponent(model.get('resourceServer'));
                    },*/
                    cell: "string",
                    editable: false
                },
                {
                    name: "type",
                    label: $.t("uma.resources.list.grid.2"),
                    cell: "string",
                    editable: false
                }
            ];

            self.data.resourceSetCollection = new ResourceSetCollection();
            self.data.resourceSetCollection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns: columns,
                collection: self.data.resourceSetCollection,
                emptyText: $.t("uma.all.grid.empty")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.resourceSetCollection,
                windowSize: 3
            });

            self.parentRender(function() {
                self.$el.find("#backgridContainer").append( grid.render().el );
                self.$el.find("#paginationContainer").append( paginator.render().el );
                self.data.resourceSetCollection.fetch({reset: true, processData: false});
                if (callback) { callback();}
            });
        }

    });

    return new ListResource();
});
