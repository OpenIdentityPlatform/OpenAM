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

/*global define, $, _, Backgrid, Backbone */

define("org/forgerock/openam/ui/uma/ResourceView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/util/BackgridUtils",
    "org/forgerock/openam/ui/uma/util/UmaUtils",
    "org/forgerock/commons/ui/common/main/Router"
], function(AbstractView, conf, eventManager, uiUtils, constants, backgridUtils, UmaUtils, router) {
    var ResourceView = AbstractView.extend({
        template: "templates/uma/ResourceTemplate.html",
        baseTemplate: "templates/policy/BaseTemplate.html",
        events: {
          "click a#revokeAll": "revokeAll",
          "click a#share": "share"
        },
       revokeAll: function() {
           // TODO Use i18n
            uiUtils.jqConfirm($.t("policy.uma.resources.show.revokeAllMessage"), function() {
                // TODO: Make a call to the policy delegate to revoke all access
            }, 325);
        },
        share: function(e) {
            e.preventDefault();
            eventManager.sendEvent(constants.EVENT_SHOW_DIALOG,
            {
                route: router.configuration.routes.resourceEdit,
                args: [this.data.resourceSet.uid] // TODO : May need to pass in more infor
            });
        },
        render: function(args, callback) {
            var self = this,
                grid,
                paginator,
                userResources,
                RevokeCell,
                SelectizeCell,
                UserResourcesCollection,
                promise = UmaUtils.getResourceSet(args[0], self.data.resourceSet);


            RevokeCell = backgridUtils.TemplateCell.extend({
                template: "templates/uma/RevokeCellTemplate.html",
                events: {
                    "click": "revoke"
                },
                revoke: function(event) {
                    event.preventDefault();
                    // this.model.get("name")
                    // TODO: Make a call to the policy delegate to revoke this access
                }
            });

            SelectizeCell = Backgrid.Cell.extend({
                className: 'selectize-cell',
                template: _.template(
                    "<select multiple class='selectize'>" +
                        "<option disabled selected value>Please select</option>" +
                        "<option value='GET'>GET</option>" +
                        "<option value='DELETE'>DELETE</option>" +
                        "<option value='PUT'>PUT</option>" +
                        "<option value='POST'>POST</option>" +
                    "</select>"),
                render: function() {
                    this.$el.html(this.template());
                    this.$("select").selectize({
                        create: false,
                        delimiter: ",",
                        dropdownParent: "body",
                        hideSelected: true,
                        persist: false,
                        plugins: ["restore_on_backspace"]
                    });
                    this.delegateEvents();

                    return this;
                }
            });

            UserResourcesCollection = Backbone.PageableCollection.extend({
                url: "/" + constants.context + "/json/applications", //TODO: /json/users/USERNAME/uma/policies > crudq
                state: {
                    pageSize: 10,
                    sortKey: "name",
                    _pagedResultsOffset : 0
                },
                queryParams: {
                    pageSize: "_pageSize",
                    sortKey: "_sortKeys",
                    _queryFilter: backgridUtils.queryFilter,
                    _pagedResultsOffset:  backgridUtils.pagedResultsOffset
                },

                parseState: backgridUtils.parseState,
                parseRecords: backgridUtils.parseRecords,
                sync: backgridUtils.sync
            });

            userResources = new UserResourcesCollection();

            grid = new Backgrid.Grid({
                columns: [{
                    name: "name",
                    label: $.t("policy.uma.resources.show.grid.0"),
                    cell: Backgrid.StringCell,
                    headerCell: backgridUtils.FilterHeaderCell,
                    editable: false
                }, {
                    name: "lastModifiedDate",
                    label: $.t("policy.uma.resources.show.grid.1"),
                    cell: backgridUtils.DatetimeAgoCell,
                    editable: false
                }, {
                    name: "permissions",
                    label: $.t("policy.uma.resources.show.grid.2"),
                    cell: SelectizeCell,
                    editable: false
                }, {
                    name: "edit",
                    label: "",
                    cell: RevokeCell,
                    editable: false
                }],
                collection: userResources,
                emptyText: $.t("policy.uma.all.grid.empty")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: userResources,
                windowSize: 3
            });

            $.when(promise).done(function(resourceSet){

                self.data.resourceSet = resourceSet;

                self.parentRender(function() {
                    self.$el.find("#backgridContainer").append( grid.render().el );
                    self.$el.find("#paginationContainer").append( paginator.render().el );
                    userResources.fetch({reset: true, processData: false});

                    if(callback) { callback(); }
                });
            });
        }
    });

    return new ResourceView();
});
