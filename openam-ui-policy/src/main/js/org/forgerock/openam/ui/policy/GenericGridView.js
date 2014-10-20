/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global window, define, $, _, document, console, sessionStorage */

define("org/forgerock/openam/ui/policy/GenericGridView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function (AbstractView, uiUtils, router, constants, eventManager) {
    var GenericGridView = AbstractView.extend({
        baseTemplate: 'templates/policy/BaseTemplate.html',

        grid: null,

        globalActionsTemplate: '',
        globalButtonSetId: '#globalActions',
        globalButtonSet: null,

        selectedItems: [],
        storageKey: '',

        checkBox: '[class*="icon-checkbox"]',
        checkBoxCheckedClass: 'icon-checkbox-checked',
        checkBoxUncheckedClass: 'icon-checkbox-unchecked',

        checkBoxFormatter: function (cellvalue, options, rowObject) {
            return "<span data-selection='" + rowObject.name + "' class='icon-checkbox-unchecked' tabindex='0' ></span>";
        },

        initBaseView: function (globalActionsTemplate, storageKey) {
            this.globalActionsTemplate = globalActionsTemplate;

            this.storageKey = constants.OPENAM_STORAGE_KEY_PREFIX + storageKey;

            var storedItems = JSON.parse(sessionStorage.getItem(this.storageKey));
            this.selectedItems = storedItems ? storedItems : [];
            this.data.itemNumber = this.selectedItems.length;
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
                this.selectedItems.push(this.data.result[rowid - 1].name);
                $target.closest('tr').addClass("highlight");
                this.grid.find('tr[id=' + rowid + ']').addClass("highlight");
            } else {
                $chB.removeClass(this.checkBoxCheckedClass).addClass(this.checkBoxUncheckedClass);
                this.selectedItems = _.without(this.selectedItems, this.data.result[rowid - 1].name);
                this.grid.jqGrid('resetSelection', rowid);
                $target.closest('tr').removeClass("highlight");
                this.grid.find('tr[id=' + rowid + ']').removeClass("highlight");
            }

            sessionStorage.setItem(this.storageKey, JSON.stringify(this.selectedItems));

            this.reloadGlobalActionsTemplate();
        },

        selectRow: function (e, rowid, rowdata) {
            if (this.selectedItems) {
                if (this.selectedItems.indexOf(rowdata.name) !== -1) {
                    var tr = this.grid.find('tr[id=' + rowid + ']');
                    tr.find(this.checkBox).removeClass(this.checkBoxUncheckedClass).addClass(this.checkBoxCheckedClass);
                    tr.addClass("highlight");
                }
            }
        },

        deleteItems: function (e, promises) {
            var self = this;

            $.when.apply($, promises).then(function () {
                self.handleItemsDelete('deleteSuccess');
            }, function () {
                self.handleItemsDelete('deleteFail');
            });
        },

        handleItemsDelete: function (message, callback) {
            sessionStorage.removeItem(this.storageKey);
            this.selectedItems = [];
            this.grid.trigger('reloadGrid');
            this.reloadGlobalActionsTemplate();

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, message);
        },

        setGridButtonSet: function () {
            this.globalButtonSet = this.$el.find(this.globalButtonSetId);
        },

        reloadGlobalActionsTemplate: function () {
            this.data.itemNumber = this.selectedItems.length;
            this.globalButtonSet.html(uiUtils.fillTemplateWithData(this.globalActionsTemplate, this.data));
        },

        isCheckBoxCellSelected: function (e) {
            var $target = $(e.target);
            return $target.is(this.checkBox) || $target.find(this.checkBox).length !== 0;
        }
    });

    return GenericGridView;
});
