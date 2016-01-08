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
 * Portions copyright 2014-2016 ForgeRock AS.
 */


define("org/forgerock/openam/ui/admin/views/realms/authorization/policySets/PolicySetsView", [
    "jquery",
    "lodash",
    "backbone",
    "backbone.paginator",
    "backgrid-filter",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView",
    "org/forgerock/openam/ui/admin/delegates/PoliciesDelegate",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/common/util/URLHelper"
], function ($, _, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator, Configuration,
             EventManager, Router, Constants, PolicySetModel, AbstractListView,
             PoliciesDelegate, BackgridUtils, RealmHelper, URLHelper) {
    return AbstractListView.extend({
        template: "templates/admin/views/realms/authorization/policySets/PolicySetsTemplate.html",
        // Used in AbstractListView
        toolbarTemplate: "templates/admin/views/realms/authorization/policySets/PolicySetsToolbarTemplate.html",
        partials: [
            "partials/util/_HelpLink.html"
        ],
        events: {
            "click #addNewPolicySet": "addNewPolicySet",
            "click #importPolicies": "startImportPolicies",
            "click #exportPolicies": "exportPolicies",
            "click [data-add-resource]" : "addResource",
            "change [name=upload]": "readImportFile"
        },
        render: function (args, callback) {
            this.realmPath = args[0];
            PoliciesDelegate.listResourceTypes().then(_.bind(function (resourceTypes) {
                if (resourceTypes.resultCount < 1) {
                    this.data.hasResourceTypes = false;
                    this.parentRender(this.renderToolbar);
                } else {
                    var self = this,
                        PolicySets,
                        columns,
                        grid,
                        paginator,
                        ClickableRow;

                    this.data.selectedItems = [];
                    this.data.hasResourceTypes = true;

                    PolicySets = Backbone.PageableCollection.extend({
                        url: URLHelper.substitute("__api__/applications"),
                        model: PolicySetModel,
                        state: BackgridUtils.getState(),
                        queryParams: BackgridUtils.getQueryParams({
                            filterName: "eq",
                            _queryFilter: [
                                "name+eq+" + encodeURIComponent('"^(?!sunAMDelegationService$).*"')
                            ]
                        }),
                        parseState: BackgridUtils.parseState,
                        parseRecords: BackgridUtils.parseRecords,
                        sync: function (method, model, options) {
                            options.beforeSend = function (xhr) {
                                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=2.0");
                            };
                            return BackgridUtils.sync(method, model, options);
                        }
                    });

                    ClickableRow = BackgridUtils.ClickableRow.extend({
                        callback: function (e) {
                            var $target = $(e.target);

                            if ($target.parents().hasClass("fr-col-btn-2")) {
                                return;
                            }

                            self.editRecord(e, this.model.id, Router.configuration.routes.realmsPolicySetEdit);
                        }
                    });

                    columns = [
                        {
                            name: "name",
                            label: $.t("console.authorization.policySets.list.grid.0"),
                            cell: BackgridUtils.TemplateCell.extend({
                                iconClass: "fa-folder",
                                template: "templates/admin/backgrid/cell/IconAndNameCell.html",
                                rendered: function () {
                                    this.$el.find("i.fa").addClass(this.iconClass);
                                }
                            }),
                            headerCell: BackgridUtils.FilterHeaderCell,
                            sortType: "toggle",
                            editable: false
                        },
                        {
                            name: "",
                            cell: BackgridUtils.TemplateCell.extend({
                                className: "fr-col-btn-2",
                                template: "templates/admin/backgrid/cell/RowActionsCell.html",
                                events: {
                                    "click .edit-row-item": "editItem",
                                    "click .delete-row-item": "deleteItem"
                                },
                                editItem: function (e) {
                                    self.editRecord(e, this.model.id, Router.configuration.routes.realmsPolicySetEdit);
                                },
                                deleteItem: function (e) {
                                    self.onDeleteClick(e, { type: $.t("console.authorization.common.policySet") },
                                        this.model.id);

                                }
                            }),
                            sortable: false,
                            editable: false
                        }
                    ];

                    this.data.items = new PolicySets();

                    grid = new Backgrid.Grid({
                        columns: columns,
                        row: ClickableRow,
                        collection: self.data.items,
                        className: "backgrid table table-hover",
                        emptyText: $.t("console.common.noResults")
                    });

                    paginator = new Backgrid.Extension.ThemeablePaginator({
                        collection: self.data.items,
                        windowSize: 3
                    });

                    this.bindDefaultHandlers();

                    this.parentRender(function () {
                        this.renderToolbar();

                        this.$el.find(".table-container").append(grid.render().el);
                        this.$el.find(".panel-body").append(paginator.render().el);

                        this.data.items.fetch({ reset: true }).done(function () {
                            if (callback) {
                                callback();
                            }
                        }).fail(function () {
                            Router.routeTo(Router.configuration.routes.realms, {
                                args: [],
                                trigger: true
                            });
                        });
                    });
                }
            }, this));
        },

        startImportPolicies: function () {
            this.$el.find("[name=upload]").trigger("click");
        },

        importPolicies: function (e) {
            PoliciesDelegate.importPolicies(e.target.result)
                .done(function () {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploaded");
                })
                .fail(function (e) {
                    var applicationNotFoundInRealm = " application not found in realm",
                        responseText = e ? e.responseText : "",
                        messages = $($.parseXML(responseText)).find("message"),
                        message = messages.length ? messages[0].textContent : "",
                        index = message ? message.indexOf(applicationNotFoundInRealm) : -1;

                    if (index > -1) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, {
                            key: "policiesImportFailed",
                            applicationName: message.slice(0, index)
                        });
                    } else {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploadFailed");
                    }
                });
        },

        readImportFile: function () {
            var file = this.$el.find("[name=upload]")[0].files[0],
                reader = new FileReader();
            reader.onload = this.importPolicies;
            if (file) {
                reader.readAsText(file, "UTF-8");
            }
        },

        exportPolicies: function () {
            var realm = this.realmPath === "/" ? "" : RealmHelper.encodeRealm(this.realmPath);
            this.$el.find("#exportPolicies").attr("href",
                Constants.host + "/" + Constants.context + "/xacml" + realm + "/policies");
        },

        addResource: function () {
            Router.routeTo(Router.configuration.routes.realmsResourceTypeNew, {
                args: [encodeURIComponent(this.realmPath)],
                trigger: true
            });
        },

        addNewPolicySet: function () {
            Router.routeTo(Router.configuration.routes.realmsPolicySetNew, {
                args: [encodeURIComponent(this.realmPath)],
                trigger: true
            });
        }
    });
});
