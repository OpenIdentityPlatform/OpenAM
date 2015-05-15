/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define $*/

define("org/forgerock/openam/ui/admin/utils/JsonEditorTheme", [
    'jsonEditor'

], function (JSONEditor) {

    var obj = {};

    obj.getTheme = function(gridColWidth1, gridColWidth2){

        // Magic number 12 is the number of colomns in the bootstrap grid.
        var gridColWidth3 = 12 - gridColWidth2,

            theme =  JSONEditor.AbstractTheme.extend({

                getSelectInput: function(options) {
                    var input = this._super(options),
                        group = document.createElement('div');

                    input.className += 'form-control';
                    group.className += 'col-sm-' + gridColWidth1;
                    group.appendChild(input);

                    return group;
                },

                setSelectOptions: function(selectGroup, options, titles) {
                    var select = selectGroup.getElementsByTagName('select')[0] || selectGroup,
                        option = null,
                        i;

                    titles = titles || [];
                    select.innerHTML = '';

                    for(i=0; i<options.length; i++) {
                        option = document.createElement('option');
                        option.setAttribute('value',options[i]);
                        option.textContent = titles[i] || options[i];
                        select.appendChild(option);
                    }
                },

                setGridColumnSize: function(el,size) {
                    // JSONEditor grid system not used, so overrided here.
                },

                afterInputReady: function(input) {
                    if (input.controlgroup) {
                        return;
                    }
                    input.controlgroup = this.closest(input,'.form-group');
                    if(this.closest(input,'.compact')) {
                        input.controlgroup.style.marginBottom = 0;
                    }
                },

                getTextareaInput: function() {
                    var el = document.createElement('textarea');
                    el.className = 'form-control';
                    return el;
                },

                getFormInputField: function(type) {
                    var input = this._super(type),
                        group = document.createElement('div');
                    if(type !== 'checkbox') {
                        input.className += 'form-control';
                    }
                    group.className += 'col-sm-' + gridColWidth1;
                    group.appendChild(input);

                    return group;
                },

                getFormInputLabel: function(text) {
                    var el = document.createElement('label');
                    el.appendChild(document.createTextNode(text));
                    el.className += ' control-label col-sm-' + gridColWidth2;
                    return el;
                },

                getFormControl: function(label, input, description) {
                    var group = document.createElement('div'),
                        div = document.createElement('div');

                    if (label && input.type === 'checkbox'){
                        group.className += ' checkbox';
                        label.appendChild(input);
                        label.style.fontSize = '14px';
                        group.style.marginTop = '0';
                        group.appendChild(label);
                        input.style.position = 'relative';
                        input.style.cssFloat = 'left';
                    } else {
                        group.className = 'form-group';
                        if (label) {
                            label.className += ' control-label col-sm-' + gridColWidth2;
                            group.appendChild(label);
                        }
                        if (input.nodeName === 'INPUT') {
                            // all Inoputs need to be wrapped in a div with the BS grid class added.
                            div.className += 'col-sm-' + gridColWidth1;
                            div.appendChild(input);
                            group.appendChild(div);
                        } else {
                            group.appendChild(input);
                        }
                    }

                    if (description) {
                        group.appendChild(description);
                    }
                    return group;
                },

                getIndentedPanel: function() {
                    var el = document.createElement('div');
                    el.className = 'well well-sm';
                    return el;
                },

                getFormInputDescription: function(text) {
                    return this.getDescription(text);
                },

                getDescription: function(text) {
                    var el = document.createElement('div'),
                        parseHtml = document.implementation.createHTMLDocument();

                    el.className = 'col-sm-offset-' + gridColWidth2 + ' col-sm-' + gridColWidth3 + ' help-block';
                    parseHtml.body.innerHTML = '<div>' + text + '</div>';
                    el.appendChild(parseHtml.body.getElementsByTagName('div')[0]);

                    return el;
                },

                getHeaderButtonHolder: function() {
                    var el = this.getButtonHolder();
                    return el;
                },

                getButtonHolder: function() {
                    var el = document.createElement('div');
                    el.className = 'btn-group';
                    return el;
                },

                getButton: function(text, icon, title) {
                    var el = this._super(text, icon, title);
                    el.className += 'btn btn-default';
                    return el;
                },

                getTable: function() {
                    var el = document.createElement('table');
                    el.className = 'table table-bordered';
                    el.style.width = 'auto';
                    el.style.maxWidth = 'none';
                    return el;
                },

                getGridRow: function() {
                    var el = document.createElement('div');
                    el.className = 'form-horizontal';
                    return el;
                },

                addInputError: function(input,text) {
                    if (!input.controlgroup){
                        return;
                    }
                    input.controlgroup.className += ' has-error';
                    if(!input.errmsg) {
                        input.errmsg = document.createElement('p');
                        input.errmsg.className = 'help-block errormsg';
                        input.controlgroup.appendChild(input.errmsg);
                    }
                    else {
                        input.errmsg.style.display = '';
                    }

                    input.errmsg.textContent = text;
                },

                removeInputError: function(input) {
                    if(!input.errmsg){
                        return;
                    }
                    input.errmsg.style.display = 'none';
                    input.controlgroup.className = input.controlgroup.className.replace(/\s?has-error/g,'');
                },

                getTabHolder: function() {
                    var el = document.createElement('div');
                    el.innerHTML = "<div class='tabs list-group col-md-2'></div><div class='col-md-10'></div>";
                    el.className = 'rows';
                    return el;
                },

                getTab: function(text) {
                    var el = document.createElement('a');
                    el.className = 'list-group-item';
                    el.setAttribute('href','#');
                    el.appendChild(text);
                    return el;
                },

                markTabActive: function(tab) {
                    tab.className += ' active';
                },

                markTabInactive: function(tab) {
                    tab.className = tab.className.replace(/\s?active/g,'');
                },

                getProgressBar: function() {
                    var min = 0,
                        max = 100,
                        start = 0,
                        container = document.createElement('div'),
                        bar = document.createElement('div');

                    container.className = 'progress';
                    bar.className = 'progress-bar';
                    bar.setAttribute('role', 'progressbar');
                    bar.setAttribute('aria-valuenow', start);
                    bar.setAttribute('aria-valuemin', min);
                    bar.setAttribute('aria-valuenax', max);
                    bar.innerHTML = start + "%";
                    container.appendChild(bar);

                    return container;
                },

                updateProgressBar: function(progressBar, progress) {
                    if (!progressBar){return;}

                    var bar = progressBar.firstChild,
                        percentage = progress + "%";
                    bar.setAttribute('aria-valuenow', progress);
                    bar.style.width = percentage;
                    bar.innerHTML = percentage;
                },

                updateProgressBarUnknown: function(progressBar) {
                    if (!progressBar){return;}

                    var bar = progressBar.firstChild;
                    progressBar.className = 'progress progress-striped active';
                    bar.removeAttribute('aria-valuenow');
                    bar.style.width = '100%';
                    bar.innerHTML = '';
                }

            });

        return theme;

    };

    return obj;
});
