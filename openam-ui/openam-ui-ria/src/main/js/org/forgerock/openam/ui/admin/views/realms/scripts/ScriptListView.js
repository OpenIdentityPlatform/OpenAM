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

define('org/forgerock/openam/ui/admin/views/realms/scripts/ScriptListView', [
    "jquery",
    "underscore",
    'backbone',
    'backgrid',
    'org/forgerock/commons/ui/common/components/Messages',
    'org/forgerock/commons/ui/common/main/AbstractView',
    'org/forgerock/commons/ui/common/main/EventManager',
    'org/forgerock/commons/ui/common/main/Router',
    'org/forgerock/commons/ui/common/util/Constants',
    'org/forgerock/commons/ui/common/util/UIUtils',
    // TODO: switch to 'org/forgerock/openam/ui/common/util/URLHelper' after PE and SE are deleted
    'org/forgerock/openam/ui/uma/util/URLHelper',
    'org/forgerock/openam/ui/common/util/BackgridUtils',
    'org/forgerock/openam/ui/admin/models/scripts/ScriptModel'
], function ($, _, Backbone, Backgrid, Messages, AbstractView, EventManager, Router, Constants, UIUtils, URLHelper, BackgridUtils, Script) {

    return AbstractView.extend({
        template: 'templates/admin/views/realms/scripts/ScriptListTemplate.html',
        toolbarTemplate: 'templates/admin/views/realms/scripts/ScriptListBtnToolbarTemplate.html',
        events: {
            'click #deleteRecords': 'deleteRecords'
        },

        render: function (args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                ClickableRow,
                FilterHeaderCell = BackgridUtils.FilterHeaderCell.extend({title: 'console.scripts.list.grid.filterBy'}),
                Scripts;

            this.data.selectedUUIDs = [];

            Scripts = Backbone.PageableCollection.extend({
                url: URLHelper.substitute('__api__/scripts'),
                model: Script,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams(),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: BackgridUtils.sync
            });

            columns = [
                {
                    name: '',
                    cell: 'select-row',
                    headerCell: 'select-all'
                },
                {
                    name: 'name',
                    label: $.t('console.scripts.list.grid.headers.0'),
                    cell: 'string',
                    headerCell: FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: 'context',
                    label: $.t('console.scripts.list.grid.headers.1'),
                    cell: 'string',
                    headerCell: FilterHeaderCell,
                    sortType: 'toggle',
                    editable: false
                },
                {
                    name: 'language',
                    label: $.t('console.scripts.list.grid.headers.2'),
                    cell: 'string',
                    headerCell: FilterHeaderCell,
                    sortType: 'toggle',
                    editable: false
                },
                {
                    name: 'description',
                    label: $.t('console.scripts.list.grid.headers.3'),
                    cell: 'string',
                    sortable: false,
                    editable: false
                }
            ];

            ClickableRow = BackgridUtils.ClickableRow.extend({
                callback: function (e) {
                    var $target = $(e.target);

                    if ($target.is('input') || $target.is('.select-row-cell')) {
                        return;
                    }

                    Router.routeTo(Router.configuration.routes.editScript, {args: [encodeURIComponent(this.model.id)], trigger: true});
                }
            });

            this.data.scripts = new Scripts();

            this.data.scripts.on('backgrid:selected', function (model, selected) {
                self.onRowSelect(model, selected);
            });

            this.data.scripts.on('backgrid:sort', BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.scripts,
                emptyText: $.t('console.scripts.list.grid.noResults')
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.scripts,
                windowSize: 3
            });

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find('#backgridContainer').append(grid.render().el);
                this.$el.find('#paginationContainer').append(paginator.render().el);

                this.data.scripts.fetch({reset: true});

                if (callback) {
                    callback();
                }
            });
        },

        deleteRecords: function (e) {
            e.preventDefault();

            var self = this,
                i = 0,
                item,
                onDestroy = function () {
                    self.data.selectedUUIDs = [];
                    self.data.scripts.fetch({reset: true});

                    UIUtils.fillTemplateWithData(self.toolbarTemplate, self.data, function (tpl) {
                        self.$el.find('#gridToolbar').html(tpl);
                    });
                },
                onSuccess = function (model, response, options) {
                    onDestroy();
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, 'scriptDeleted');
                },
                onError = function (model, response, options) {
                    onDestroy();
                    Messages.messages.addMessage({message: response.responseJSON.message, type: 'error'});
                };

            for (; i < this.data.selectedUUIDs.length; i++) {
                item = this.data.scripts.get(this.data.selectedUUIDs[i]);

                item.destroy({
                    success: onSuccess,
                    error: onError
                });
            }
        },

        onRowSelect: function (model, selected) {
            if (selected) {
                if (!_.contains(this.data.selectedUUIDs, model.id)) {
                    this.data.selectedUUIDs.push(model.id);
                }
            } else {
                this.data.selectedUUIDs = _.without(this.data.selectedUUIDs, model.id);
            }

            this.renderToolbar();
        },

        renderToolbar: function () {
            this.$el.find('#gridToolbar').html(UIUtils.fillTemplateWithData(this.toolbarTemplate, this.data));
        }
    });
});