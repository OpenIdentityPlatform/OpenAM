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
 * Copyright 2015-2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/utils/JSONEditorTheme", [
    "jquery",
    "lodash",
    "jsonEditor"
], function ($, _, JSONEditor) {
    var obj = {};

    obj.getTheme = function (gridColWidth1, gridColWidth2) {
        // Magic number 12 is the number of colomns in the bootstrap grid.
        var gridColWidth3 = 12 - gridColWidth2,
            theme = JSONEditor.AbstractTheme.extend({

                getSelectInput: function (options) {
                    var input = this._super(options);
                    input.className += "form-control";

                    return input;
                },

                setSelectOptions: function (selectGroup, options, titles) {
                    var select = selectGroup.getElementsByTagName("select")[0] || selectGroup,
                        option = null,
                        i;

                    titles = titles || [];
                    select.innerHTML = "";

                    for (i = 0; i < options.length; i++) {
                        option = document.createElement("option");
                        option.setAttribute("value", options[i]);
                        option.textContent = titles[i] || options[i];
                        select.appendChild(option);
                    }
                },

                setGridColumnSize: function () {
                    // JSONEditor grid system not used, so overridden here.
                },

                afterInputReady: function (input) {
                    if (input.controlgroup) {
                        return;
                    }
                    input.controlgroup = this.closest(input, ".form-group");
                    if (this.closest(input, ".compact")) {
                        input.controlgroup.style.marginBottom = 0;
                    }
                },

                getTextareaInput: function (placeholder) {
                    var el = document.createElement("textarea");
                    el.className = "form-control";
                    if (placeholder) {
                        el.setAttribute("placeholder", placeholder);
                    }
                    return el;
                },

                getFormInputField: function (type, placeholder) {
                    var input = this._super(type);
                    if (type === "checkbox") {
                        input.style.marginTop = "12px";
                    } else {
                        input.className += "form-control";
                    }
                    if (placeholder) {
                        input.setAttribute("placeholder", placeholder);
                    }
                    input.setAttribute("autocomplete", "off");
                    return input;
                },

                getFormInputLabel: function (text) {
                    var el = document.createElement("label");
                    el.appendChild(document.createTextNode(text));
                    el.className += ` control-label col-sm-${gridColWidth2}`;
                    return el;
                },

                getFormControl: function (label, input, description) {
                    var group = document.createElement("div"),
                        div = document.createElement("div");

                    if (label && $(input).find("input").prop("type") === "checkbox") {
                        group.className += " checkbox";
                        input.style.marginTop = "12px";
                    }

                    group.className = "form-group";

                    if (label) {
                        label.className += ` control-label col-sm-${gridColWidth2}`;
                        group.appendChild(label);
                    }

                    if (input.nodeName.toLowerCase() === "input" || input.nodeName.toLowerCase() === "select") {
                        // All Inputs need to be wrapped in a div with the BS grid class added.
                        div.className += `col-sm-${gridColWidth1}`;
                        div.appendChild(input);
                        group.appendChild(div);
                    } else {
                        group.appendChild(input);
                    }

                    if (description) {
                        group.appendChild(description);
                    }
                    return group;
                },

                getCheckboxLabel: function (text) {
                    return this.getFormInputLabel(text);
                },

                getIndentedPanel: function () {
                    return document.createElement("div");
                },

                getFormInputDescription: function (text) {
                    return this.getDescription(text);
                },

                getDescription: function (text) {
                    var el = document.createElement("div"),
                        parseHtml = document.implementation.createHTMLDocument("");

                    el.className = `col-sm-offset-${gridColWidth2} col-sm-${gridColWidth3} help-block`;
                    parseHtml.body.innerHTML = `<div class='wordwrap'>${text}</div>`;
                    el.appendChild(parseHtml.body.getElementsByTagName("div")[0]);

                    return el;
                },

                getHeaderButtonHolder: function () {
                    return this.getButtonHolder();
                },

                getButtonHolder: function () {
                    var el = document.createElement("div");
                    el.className = "btn-group";
                    return el;
                },

                getButton: function (text, icon, title) {
                    var el = this._super(text, icon, title);
                    el.className += "btn btn-default";
                    return el;
                },

                getInlineButton: function (text, icon, title) {
                    var el = this._super(text, icon, title);
                    el.className += "btn btn-link delete-row-item";
                    return el;
                },

                getTable: function () {
                    var el = document.createElement("table");
                    el.className = "table table-bordered";
                    el.style.width = "auto";
                    el.style.maxWidth = "none";
                    return el;
                },

                getGridRow: function () {
                    var el = document.createElement("div");
                    el.className = "form-horizontal";
                    return el;
                },

                addInputError: function (input, text) {
                    if (!input.controlgroup) {
                        return;
                    }
                    input.controlgroup.className += " has-error";
                    if (!input.errmsg) {
                        input.errmsg = document.createElement("p");
                        input.errmsg.className = "help-block errormsg";
                        input.controlgroup.appendChild(input.errmsg);
                    } else {
                        input.errmsg.style.display = "";
                    }

                    input.errmsg.textContent = text;
                },

                removeInputError: function (input) {
                    if (!input.errmsg) {
                        return;
                    }
                    input.errmsg.style.display = "none";
                    input.controlgroup.className = input.controlgroup.className.replace(/\s?has-error/g, "");
                },

                getTabHolder: function () {
                    var el = document.createElement("div");
                    el.innerHTML = "<div class=tabs 'list-group col-md-2'></div><div class='col-md-10'></div>";
                    el.className = "rows";
                    return el;
                },

                getTab: function (text) {
                    var el = document.createElement("a");
                    el.className = "list-group-item";
                    el.setAttribute("href", "#");
                    el.appendChild(text);
                    return el;
                },

                markTabActive: function (tab) {
                    tab.className += " active";
                },

                markTabInactive: function (tab) {
                    tab.className = tab.className.replace(/\s?active/g, "");
                },

                getProgressBar: function () {
                    var min = 0,
                        max = 100,
                        start = 0,
                        container = document.createElement("div"),
                        bar = document.createElement("div");

                    container.className = "progress";
                    bar.className = "progress-bar";
                    bar.setAttribute("role", "progressbar");
                    bar.setAttribute("aria-valuenow", start);
                    bar.setAttribute("aria-valuemin", min);
                    bar.setAttribute("aria-valuenax", max);
                    bar.innerHTML = `${start}%`;
                    container.appendChild(bar);

                    return container;
                },

                updateProgressBar: function (progressBar, progress) {
                    if (!progressBar) {
                        return;
                    }

                    var bar = progressBar.firstChild,
                        percentage = `${progress}%`;
                    bar.setAttribute("aria-valuenow", progress);
                    bar.style.width = percentage;
                    bar.innerHTML = percentage;
                },

                updateProgressBarUnknown: function (progressBar) {
                    if (!progressBar) {
                        return;
                    }

                    var bar = progressBar.firstChild;
                    progressBar.className = "progress progress-striped active";
                    bar.removeAttribute("aria-valuenow");
                    bar.style.width = "100%";
                    bar.innerHTML = "";
                },

                getFirstColumnWrapper: function () {
                    var wrapper = document.createElement("div");
                    wrapper.className = `col-sm-${gridColWidth1}`;
                    return wrapper;
                },

                getSecondColumnWrapper: function () {
                    var wrapper = document.createElement("div");
                    wrapper.className = `col-sm-offset-1 col-sm-${(gridColWidth2 - 1)}`;
                    return wrapper;
                },

                addError: function (element) {
                    $(element).addClass("has-error");
                },

                removeError: function (element) {
                    $(element).removeClass("has-error");
                },

                addBorder: function (element) {
                    element.style.border = "solid 1px rgb(204, 204, 204)";
                    element.style.marginBottom = "15px";
                },

                getHeader: function (text) {
                    const el = document.createElement("h3");

                    el.className = "block-header";
                    el.setAttribute("data-header", true);
                    if (typeof text === "string") {
                        el.textContent = text;
                    }
                    return el;
                },

                getMapHeader: function (text) {
                    var el = document.createElement("div"), header = document.createElement("label");
                    el.appendChild(header);
                    if (typeof text === "string") {
                        header.textContent = text;
                    }
                    el.style.display = "inline-block";
                    el.className = "col-sm-offset-1";
                    return el;
                },

                getKeyFormInputField: function () {
                    return this.getFormInputField("text", $.t("common.form.key"));
                },

                getValueFormInputField: function () {
                    return this.getFormInputField("text", $.t("common.form.value"));
                },

                getInputId: function () {
                    return _.uniqueId();
                },

                getModal: function () {
                    var el = document.createElement("div");
                    el.className = "form-group";
                    return el;
                }
            });

        return theme;
    };

    return obj;
});
