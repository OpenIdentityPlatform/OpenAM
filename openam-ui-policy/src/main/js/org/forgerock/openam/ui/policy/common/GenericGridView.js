/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

/**
 * @author Eugenia Sergueeva
 */

/*global define, $, _, sessionStorage */

define("org/forgerock/openam/ui/policy/common/GenericGridView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages"
], function (AbstractView, uiUtils, dateUtil, router, constants, eventManager, messages) {
    var GenericGridView = AbstractView.extend({
        noBaseTemplate: true,

        checkBox: '[class*="icon-checkbox"]',
        checkBoxCheckedClass: 'icon-checkbox-checked',
        checkBoxUncheckedClass: 'icon-checkbox-unchecked',

        checkBoxFormatter: function (cellvalue, options, rowObject) {
            return "<span data-selection='" + rowObject.name + "' class='icon-checkbox-unchecked' tabindex='0' ></span>";
        },

        render: function (options, callback) {
            var self = this, storedItems, dateRangeFilter;

            this.element = options.element;
            this.template = options.tpl;
            this.gridId = options.gridId;
            this.rowUid = options.rowUid;

            this.data[this.gridId] = {};

            this.actionsTpl = options.actionsTpl;
            this.storageKey = constants.OPENAM_STORAGE_KEY_PREFIX + options.additionalOptions.storageKey;

            storedItems = JSON.parse(sessionStorage.getItem(this.storageKey));
            this.selectedItems = storedItems ? storedItems : [];

            dateRangeFilter = { groupOp: "AND", rules: [] };
            dateRangeFilter.rules.push({ field: "creationDate", op: "gt", data: "" });
            dateRangeFilter.rules.push({ field: "creationDate", op: "lt", data: "" });
            dateRangeFilter.rules.push({ field: "lastModifiedDate", op: "gt", data: "" });
            dateRangeFilter.rules.push({ field: "lastModifiedDate", op: "lt", data: "" });

            $.extend(true, options.initOptions, { postData: { filters: JSON.stringify(dateRangeFilter)} });

            this.parentRender(function () {
                this.actions = this.$el.find('.global-actions');
                this.grid = uiUtils.buildJQGrid(this, options.gridId, options.initOptions, options.additionalOptions, callback);

                if (options.additionalOptions.callback) {
                    options.additionalOptions.callback.apply(this);
                }

                this.reloadGlobalActionsTemplate();
            });
        },

        getSelectedRowId: function (e) {
            return $(e.target).closest('tr').attr('id');
        },

        onRowSelect: function (rowid, status, e) {
            var $target = $(e.target),
                $chB = $target.is(this.checkBox) ? $target : $target.find(this.checkBox),
                checked = $chB.hasClass(this.checkBoxCheckedClass);

            if (!checked) {
                $chB.removeClass(this.checkBoxUncheckedClass).addClass(this.checkBoxCheckedClass);
                this.selectedItems.push(this.data[this.gridId].result[rowid - 1][this.rowUid]);
                $target.closest('tr').addClass("highlight");
                this.grid.find('tr[id=' + rowid + ']').addClass("highlight");
            } else {
                $chB.removeClass(this.checkBoxCheckedClass).addClass(this.checkBoxUncheckedClass);
                this.selectedItems = _.without(this.selectedItems, this.data[this.gridId].result[rowid - 1][this.rowUid]);
                this.grid.jqGrid('resetSelection', rowid);
                $target.closest('tr').removeClass("highlight");
                this.grid.find('tr[id=' + rowid + ']').removeClass("highlight");
            }

            sessionStorage.setItem(this.storageKey, JSON.stringify(this.selectedItems));

            this.reloadGlobalActionsTemplate();
        },

        selectRow: function (e, rowid, rowdata) {
            if (this.selectedItems.length) {
                if (_.contains(this.selectedItems, rowdata[this.rowUid])) {
                    var tr = this.grid.find('tr[id=' + rowid + ']');
                    tr.find(this.checkBox).removeClass(this.checkBoxUncheckedClass).addClass(this.checkBoxCheckedClass);
                    tr.addClass("highlight");
                }
            }
        },

        deleteItems: function (e, promises) {
            var self = this;

            $.when.apply($, promises)
                .always(function () {
                    self.handleItemsDelete();
                })
                .done(function () {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, 'deleteSuccess');
                })
                .fail(function (xhr) {
                    messages.messages.addMessage({message: xhr.responseJSON.message, type: 'error'});
                });
        },

        handleItemsDelete: function () {
            sessionStorage.removeItem(this.storageKey);
            this.selectedItems = [];
            this.grid.trigger('reloadGrid');
            this.reloadGlobalActionsTemplate();
        },

        reloadGlobalActionsTemplate: function () {
            this.data[this.gridId].selected = this.selectedItems.length;
            this.actions.html(uiUtils.fillTemplateWithData(this.actionsTpl, this.data));
        },

        isCheckBoxCellSelected: function (e) {
            var $target = $(e.target);
            return $target.is(this.checkBox) || $target.find(this.checkBox).length !== 0;
        },

        datePicker: function(gridView, elem) {
            var view = gridView;
            $(elem).datepicker({onSelect: function(){
                if (this.id.substr(0, 3) === "gs_") {
                    // in case of searching toolbar
                    view.grid[0].triggerToolbar();
                } else {
                    // refresh the filter in case of searching dialog
                    $(this).trigger('change');
                }
            }});
        },

        serializeDataToFilter: function (postedData, colNames) {
            var filter = '', filterDataToDate, nextDay;

            _.each(colNames, function (element, index, list) {
                if (postedData[element]) {
                    if (filter.length > 0) {
                        filter += ' AND ';
                    }
                    filterDataToDate = new Date(postedData[element]);
                    if (dateUtil.isDateValid(filterDataToDate)) {
                        nextDay = new Date( filterDataToDate.getTime() + 24 * 60 * 60 * 1000 );
                        filter = filter.concat(element, ' gt ', filterDataToDate.getTime().toString(), ' AND ',  element, ' lt ', nextDay.getTime().toString());
                    } else {
                        filter = filter.concat(element, ' eq "*', postedData[element], '*"');
                    }
                }
                delete postedData[element];
            });

            return filter;
        }
    });

    return GenericGridView;
});