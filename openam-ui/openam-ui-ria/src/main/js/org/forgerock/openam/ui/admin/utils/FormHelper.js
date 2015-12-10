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

/**
 * @module org/forgerock/openam/ui/admin/utils/FormHelper
 */
define("org/forgerock/openam/ui/admin/utils/FormHelper", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/components/Messages"
], function ($, _, BootstrapDialog, Messages) {

    var obj = {};

    /**
     * Binds a promise representing a save to a button element, visualising it's state.
     * <p>
     * Intented to be used in conjunction with the <code>_JSONSchemaFooter.html</code> partial.
     * @param  {Promise} promise Save promise. Usually a promise from an AJAX request
     * @param  {HTMLElement} element The button element visualising the promise's state
     * @example
     * clickHandler: function (event) {
     *   var promise = Service.update(this.data);
     *
     *   FormHelper.bindSavePromiseToElement(promise, event.currentTarget);
     * }
     */
    obj.bindSavePromiseToElement = function (promise, element) {
        element = $(element);
        element.prop("disabled", true);
        element.width(element.width());

        var span = element.find("span"),
            text = span.text(),
            elementClass = element.attr("class");

        span.fadeOut(300, function () {
            span.empty();
            span.removeClass().addClass("fa fa-refresh fa-spin");
            span.fadeIn(300);

            promise.done(function () {
                span.removeClass().addClass("fa fa-check fa-fw");
            }).fail(function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: response
                });
                span.removeClass().addClass("fa fa-times fa-fw");
                element.removeClass().addClass("btn btn-danger");
            }).always(function () {
                _.delay(function () {
                    span.fadeOut(300, function () {
                        span.removeClass().text(text);
                        element.removeClass().addClass(elementClass).prop("disabled", false);
                        span.fadeIn(300);
                    });
                }, 1000);
            });
        });
    };

    /**
     * Shows a confirm dialog before deleting and call delete callback if needed.
     * @param  {object} msg The object to define warning text
     * @param  {string} msg.type The parameter for the default confirm message ("Are you sure that you want to
     *                           delete this _type_?")
     * @param  {string} msg.message The text which overrides default confirm message
     * @param  {function} deleteCallback The callback to remove the edited entity
     * @example
     * clickHandler: function (event) {
     *   event.preventDefault();
     *   FormHelper.showConfirmationBeforeDeleting({type: "console.scripts.edit.script"}, deleteEntity);
     * }
     */
    obj.showConfirmationBeforeDeleting = function (msg, deleteCallback) {
        _.defaults(msg, { message: $.t("console.common.confirmDeleteText", { type: $.t(msg.type) }) });
        BootstrapDialog.confirm({
            type: BootstrapDialog.TYPE_DANGER,
            cssClass: "delete-confirmation",
            title: $.t("console.common.confirmDelete"),
            message: msg.message,
            btnOKLabel: $.t("common.form.delete"),
            btnOKClass: "btn-danger",
            callback: function (result) {
                if (result && deleteCallback) {
                    deleteCallback();
                }
            }
        });
    };

    /**
     * Sets active tab whose ID indicated in the variable view.activeTabId.
     * @param  {Object} view Backbone view with tabs
     * @param  {string} view.activeTabId ID tab which you want to make active
     * @example
     * FormHelper.setActiveTab(self);
     */
    obj.setActiveTab = function (view) {
        if (view && view.activeTabId) {
            view.$el.find(".nav-tabs a[href='" + view.activeTabId + "']").tab("show");
        }
    };

    return obj;
});
