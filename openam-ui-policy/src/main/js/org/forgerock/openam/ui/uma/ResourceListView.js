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

define("org/forgerock/openam/ui/uma/ResourceListView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/util/BackgridUtils",
    "org/forgerock/commons/ui/common/main/Router"
], function(AbstractView, conf, eventManager, uiUtils, constants, backgridUtils, router) {

    var ResourceListView = AbstractView.extend({
        template: "templates/uma/ResourceListTemplate.html",
        baseTemplate: 'templates/policy/BaseTemplate.html',
        events: {
            'click td': 'openPolicy'
        },

        render: function(args, callback) {

            var self = this,
                columns,
                grid,
                paginator,
                ResourceSetCollection;

            ResourceSetCollection = Backbone.PageableCollection.extend({
                url: "/" + constants.context + "/json/users/" + conf.loggedUser.userid.id + '/uma/policies',
                state: {
                    pageSize: 10,
                    sortKey: "name"
                },
                queryParams: {
                    pageSize: "_pageSize",
                    sortKey: "_sortKeys",
                    _queryFilter: 'resourceServer+eq+"agent"'
                    /* TODO : Temp until endpoint working
                    // backgridUtils.queryFilter=true,
                    //_pagedResultsOffset:  backgridUtils.pagedResultsOffset*/
                },

                parseState: backgridUtils.parseState,
                parseRecords: backgridUtils.parseRecords,
                sync: backgridUtils.sync
            });

            columns = [
                {
                    name: "name",
                    label: $.t("policy.uma.resources.list.grid.0"),
                    cell: backgridUtils.UriExtCell,
                    headerCell: backgridUtils.FilterHeaderCell,
                    href: function(rawValue, formattedValue, model){
                        return "#uma/resources/" +  model.get('policyId') + "/activity/";
                    },
                    model: true,
                    editable: false
                }/*,
                {
                    name: "lastModifiedDate",
                    label: $.t("policy.uma.resources.list.grid.2"),
                    cell: "datetime",
                    editable: false
                },
                {
                    name: "resources",
                    label: $.t("policy.uma.resources.list.grid.3"),
                    editable: false,
                    cell: backgridUtils.UriExtCell,
                    href: function(rawValue, formattedValue, model){
                        return "#uma/apps/" + formattedValue;
                    }
                }*/

            ];

            self.data.resourceSetCollection = new ResourceSetCollection();

            grid = new Backgrid.Grid({
                columns: columns,
                collection: self.data.resourceSetCollection,
                emptyText: $.t("policy.uma.all.grid.empty")
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
        },

        openPolicy: function(e) {
            e.preventDefault();
            this.data.policy = $(e.currentTarget).data();
            router.routeTo( router.configuration.routes.resourceActivity, {args: [this.data.policy.policyId], trigger: true});
        }

    });

    return new ResourceListView();
});
