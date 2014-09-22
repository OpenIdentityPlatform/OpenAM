/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

/*global require, define, QUnit, $ */

define([
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/policy/login/LoginHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/openam/ui/policy/EditApplicationView",
    "org/forgerock/openam/ui/policy/EditPolicyView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ManageApplicationsView",
    "org/forgerock/openam/ui/policy/ManagePoliciesView",
    "org/forgerock/openam/ui/policy/ActionsView",
    "org/forgerock/openam/ui/policy/ReviewInfoView",
    "org/forgerock/openam/ui/policy/EditEnvironmentView",
    "org/forgerock/openam/ui/policy/EditSubjectView",
    "org/forgerock/openam/ui/policy/ManageEnvironmentsView",
    "org/forgerock/openam/ui/policy/ManageSubjectsView",
    "org/forgerock/openam/ui/policy/OperatorRulesView"
], function (eventManager, constants, conf, router, loginHelper, uiUtils, policyDelegate, editAppView, 
             editPolView, resListView, addNewResView, manageAppsView,
             managePolView, actionsView, reviewInfoView) {
    return {
        executeAll: function (server) {

            module('Manage applications page');

            QUnit.asyncTest("Edit Application", function () {
                editAppView.element = $("<div>")[0];

                editAppView.render(['iPlanetAMWebAgentService'], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="" placeholder="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td class="res-name">{{this}}</td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each options.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];

                    QUnit.ok(editAppView.accordion.getActive() === 2, "Last step of accordion is selected");
                    QUnit.ok(editAppView.$el.find('#backButton').length, "Back button is available");

                    var app = editAppView.data.entity,
                        options = editAppView.data.options;

                    // Step 1
                    QUnit.ok(editAppView.$el.find('#appName').val() === app.name, "Name is set");
                    QUnit.ok(editAppView.$el.find('#appDescription').val() === (app.description ? app.description : ''), "Description is set");
                    QUnit.ok(editAppView.$el.find('#appRealm').val() === app.realm, "Realm is set");

                    // Step 2
                    resListView.render([], function () {
                        var resources = resListView.$el.find('.res-name');
                        QUnit.ok(resources.length === app.resources.length, "Correct number of resources is displayed");

                        var resourcesPresent = true;
                        _.each(resources, function (val, key, list) {
                            resourcesPresent = resourcesPresent && _.contains(app.resources, val.innerHTML);
                        });
                        QUnit.ok(resourcesPresent, "Resources are displayed correctly");
                    });

                    addNewResView.render([], function () {
                        var resPattern = addNewResView.$el.find('#urlResource');

                        QUnit.ok(resPattern.find('option').length === 1, "One available option for resource pattern");
                        QUnit.ok(resPattern.find('option').val() === '*', "Selected value for resource pattern is *");

                        var oldResourcesNumber = app.resources.length;
                        addNewResView.$el.find('.resource-url-part').val('1234567890');
                        addNewResView.$el.find('#addResource').trigger('click');

                        QUnit.ok(oldResourcesNumber + 1 === app.resources.length, "New resource was added into an object");
                        QUnit.ok(resListView.$el.find('.resources-grid').find('tbody tr').last()[0].children[1].innerHTML, "New resource was added to the table");

                        // Delete resources
                        oldResourcesNumber = app.resources.length;
                        addNewResView.$el.find('.resource-url-part').val('del1');
                        addNewResView.$el.find('#addResource').trigger('click');

                        addNewResView.$el.find('.resource-url-part').val('del2');
                        addNewResView.$el.find('#addResource').trigger('click');

                        var selectResource = resListView.$el.find('[data-resource-index]');
                        $(selectResource[0]).attr('checked', 'checked');
                        $(selectResource[1]).attr('checked', 'checked');

                        resListView.$el.find('#deleteResources').trigger('click');
                        QUnit.ok(oldResourcesNumber === app.resources.length, "Manipulation with resources: " +
                            "added 2, deleted 2, the remaining number of resources is the same");

                        // Reload review step
                        $('#reviewInfo', editAppView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewApplicationStepTemplate.html', editAppView.data));
                    });


                    // Step 3
                    $('#reviewInfo', editAppView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewApplicationStepTemplate.html', editAppView.data, function () {
                        QUnit.ok(editAppView.$el.find('#reviewName').html() === app.name, "Correct name is displayed in the review step");

                        if ((!editAppView.$el.find('#reviewDescr').html()) && (!app.description)) {
                            // both are undefined.
                            QUnit.ok(true, "Correct description is displayed in the review step");
                        } else {
                            QUnit.ok(editAppView.$el.find('#reviewDesc').html() === (app.description ? app.description : ''), "Correct description is displayed in the review step");
                        }

                        // Realms
                        if (app.realm.length) {
                            var realms = [];
                             _.each(editAppView.$el.find('ul#reviewRealm').find('li'), function (value, key) {
                                realms[key] = value.innerHTML;
                            });
                             // Currently only one realm, so hardcoded to [0];
                            QUnit.ok(realms[0] === app.realm, "Correct realm is displayed in the review step");
                        }

                        // Resources
                        if (app.resources.length) {
                            var resources = [];
                            _.each(editAppView.$el.find('ul#reviewRes').find('li'), function (value, key) {
                                resources[key] = value.innerHTML;
                            });

                            QUnit.ok(_.isEqual(resources, app.resources), "Correct resources are displayed in the review step");
                        }
                        QUnit.start();
                    }));


                });
            });

            QUnit.asyncTest("Create new application", function () {
                editAppView.element = $("<div>")[0];

                editAppView.render([], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="" placeholder="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td class="res-name">{{this}}</td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each options.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];

                    QUnit.ok(editAppView.accordion.getActive() === 0, "First step of accordion is selected");
                    QUnit.ok(editAppView.$el.find('#backButton').length, "Back button is available");

                    var app = editAppView.data.entity;

                    // Step 1
                    QUnit.ok(editAppView.$el.find('#appName').val() === '', "Name is empty");
                    QUnit.ok(editAppView.$el.find('#appDescription').val() === '', "Description is empty");
                    QUnit.ok(editAppView.$el.find('#appRealm').val() === '', "Realm is empty");

                    // Step 2
                    resListView.render([], function () {
                        var resources = resListView.$el.find('.res-name');
                        QUnit.ok(resources.length === 0, "No resources present");

                        QUnit.start();
                    });
                });
            });

            QUnit.asyncTest("List all applications", function () {
                manageAppsView.element = $("<div>")[0];

                $("#qunit-fixture").append(manageAppsView.element);

                manageAppsView.render([], function () {
                    var table = $('#manageApps', manageAppsView.$el),
                        postedData = table.jqGrid('getGridParam', 'postData'),
                        rowData = table.jqGrid('getRowData'),
                        recordsTotal = table.jqGrid('getGridParam', 'records'),
                        totalNumberOfPages = table.jqGrid('getGridParam', 'lastpage'),
                        recordsPerPage = table.jqGrid('getGridParam', 'rowNum'),
                        rowList = table.jqGrid('getGridParam', 'rowList'),
                        remaining = table.jqGrid('getGridParam', 'userData').remaining;

                    QUnit.ok(rowData.length > 0, "At least one application listed in the table");
                    QUnit.ok(rowData.length === table.find("tr[id]").length, "Number of rows in grid match number displayed");

                    QUnit.ok(table.jqGrid('getGridParam', 'colNames').length === table.find("tr[id]")[0].children.length,
                        'Total number of columns displayed matches number of columns requested');

                    // sorting
                    QUnit.ok(manageAppsView.$el.find('#manageApps_name').find('.s-ico').length === 1,
                        'Sort icon is present for the name column');

                    QUnit.ok(manageAppsView.$el.find('#manageApps_name').find('span[sort=desc]').hasClass('ui-state-disabled'),
                        'Name is sorted in ascending order');

                    // Pagination
                    QUnit.ok($('#appsPager', manageAppsView.$el).length === 1, 'Pager is present');

                    QUnit.ok(rowData.length + postedData._pagedResultsOffset + remaining === recordsTotal,
                        'Total number of records is calculated correctly');

                    QUnit.ok(recordsPerPage >= rowData.length,
                        'Number of rows in grid is less than or equal to number of rows requested');

                    if (recordsTotal > recordsPerPage) {
                        QUnit.ok(totalNumberOfPages === recordsTotal % recordsPerPage === 0 ?
                            recordsTotal / recordsPerPage : Math.floor(recordsTotal / recordsPerPage) + 1,
                            'Total number of pages is calculated correctly');
                    } else {
                        QUnit.ok(totalNumberOfPages === 1,
                            'Total number of pages is calculated correctly');
                    }

                    // Show/hide columns
                    QUnit.ok($('.navtable', manageAppsView.$el).length === 1, 'Columns Button is available');
                    QUnit.start();
                });
            });

            module('Manage policies page');

            QUnit.asyncTest("Edit Policy", function () {
                editPolView.element = $("<div>")[0];

                editPolView.render(['sunIdentityServerLibertyPPService', 'qwwqqw'], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="" placeholder="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td class="res-name">{{this}}</td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each options.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>'),
                        actionsViewEl = $('<div>').append('<thead><tr class="header-titles"><th><input class="toggle-all-actions" type="checkbox"/></th><th><a href="">Action</a></th><th><a href="">Permission</a></th></tr></thead><tbody>{{#each entity.actions}}<tr><td><input class="toggle-action" type="checkbox"{{#if selected}}checked{{/if}}data-action-name="{{action}}"/></td><td class="action-name">{{action}}</td><td><div class="group-field-row"><input type="radio" name="action{{@index}}" id="allow{{@index}}" value="Allow" data-action-name="{{action}}"{{#if value}}checked{{/if}}/><label for="allow{{@index}}">Allow</label><input type="radio" name="action{{@index}}" id="deny{{@index}}" value="Deny" data-action-name="{{action}}"{{#unless value}}checked{{/unless}}/><label for="deny{{@index}}">Deny</label></div></td></tr>{{/each}}</tbody>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];
                    actionsView.element = actionsViewEl[0];

                    QUnit.ok(editPolView.accordion.getActive() === 5, "Last step of accordion is selected");
                    QUnit.ok(editPolView.$el.find('#cancelButton').length, "Cancel button is available");

                    var pol = editPolView.data.entity,
                        options = editPolView.data.options;

                    // Step 1
                    QUnit.ok(editPolView.$el.find('#policyName').val() === pol.name, "Name is set");
                    QUnit.ok(editPolView.$el.find('#description').val() === (pol.description ? pol.description : ''), "Description is set");

                    // Step 2
                    resListView.render([], function () {
                        var resources = resListView.$el.find('.res-name');
                        QUnit.ok(resources.length === pol.resources.length, "Correct number of resources is displayed");

                        var resourcesPresent = true;
                        _.each(resources, function (val, key, list) {
                            resourcesPresent = resourcesPresent && _.contains(pol.resources, val.innerHTML);
                        });
                        QUnit.ok(resourcesPresent, "Resources are displayed correctly");
                    });

                    addNewResView.render([], function () {
                        var resPattern = addNewResView.$el.find('#urlResource');

                        QUnit.ok(resPattern.find('option').length === options.resourcePatterns.length, "Correct number of resource patterns");

                        var resPatternPresent = true;
                        _.each(resPattern.find('option'), function (val, key, list) {
                            resPatternPresent = resPatternPresent && _.contains(options.resourcePatterns, val.innerHTML);
                        });
                        QUnit.ok(resPatternPresent, "Resource Patters are displayed correctly");

                        var oldResourcesNumber = pol.resources.length;
                        addNewResView.$el.find('.resource-url-part').val('1234567890');
                        addNewResView.$el.find('#addResource').trigger('click');

                        QUnit.ok(oldResourcesNumber + 1 === pol.resources.length, "New resource was added into an object");
                        QUnit.ok(resListView.$el.find('.resources-grid').find('tbody tr').last()[0].children[1].innerHTML, "New resource was added to the table");

                        // Delete resources
                        oldResourcesNumber = pol.resources.length;
                        addNewResView.$el.find('.resource-url-part').val('del1');
                        addNewResView.$el.find('#addResource').trigger('click');

                        addNewResView.$el.find('.resource-url-part').val('del2');
                        addNewResView.$el.find('#addResource').trigger('click');

                        var selectResource = resListView.$el.find('[data-resource-index]');
                        $(selectResource[0]).attr('checked', 'checked');
                        $(selectResource[1]).attr('checked', 'checked');

                        resListView.$el.find('#deleteResources').trigger('click');
                        QUnit.ok(oldResourcesNumber === pol.resources.length, "Manipulation with resources: " +
                            "added 2, deleted 2, the remaining number of resources is the same");

                        // Reload review step
                        $('#reviewPolicyInfo', editPolView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewPolicyStepTemplate.html', editPolView.data));
                    });

                    // Step 3
                    actionsView.render([], function () {
                        // Correct available actions are displayed
                        var availableActions = actionsView.data.options.availableActions,
                            actionsCells = actionsView.$el.find('.action-name');
                        QUnit.ok(availableActions.length === actionsCells.length, "Correct number of actions is displayed");

                        var actionsPresent = true;
                        _.each(actionsCells, function (val, key, list) {
                            actionsPresent = actionsPresent && _.find(availableActions, function (action) {
                                return action.action === val.innerHTML;
                            });
                        });
                        QUnit.ok(actionsPresent, "Actions are displayed correctly (available for selected application type)");

                        // Toggle all actions
                        var toggleAll = actionsView.$el.find('.toggle-all-actions').prop('checked', true),
                            allChecked = true;

                        actionsView.toggleAllActions({target: actionsView.$el.find('.toggle-all-actions')[0]});

                        _.each(actionsView.data.entity.actions, function (action) {
                            allChecked = allChecked && action.selected;
                        });
                        QUnit.ok(allChecked, "All actions are marked as selected in a JS object after Toggle All checkbox is selected");

                        toggleAll.prop('checked', false);
                        actionsView.toggleAllActions({target: actionsView.$el.find('.toggle-all-actions')[0]});

                        allChecked = true;
                        _.each(actionsView.data.entity.actions, function (action) {
                            allChecked = allChecked && !action.selected;
                        });
                        QUnit.ok(!allChecked, "All actions are marked as deselected in a JS object after Toggle All checkbox is deselected");

                        // Action permissions
                        var permissions = actionsView.$el.find('input[type=radio][data-action-name]:checked'),
                            correctPermissions = true;
                        _.each(permissions, function (val, key, list) {
                            correctPermissions = correctPermissions &&
                                _.find(actionsView.data.entity.actions, function (action) {
                                    return action.action === val.getAttribute('data-action-name') &&
                                        action.value === (val.value === 'Allow');
                                });
                        });
                        QUnit.ok(correctPermissions, "All permissions are selected correctly");

                        // Selecting individual actions
                        var toggleSingleAction = actionsView.$el.find('.toggle-action');
                        _.each(toggleSingleAction, function (val, key, list) {
                            $(val).attr('checked', 'checked').trigger('click');
                        });
                        allChecked = true;
                        _.each(actionsView.data.entity.actions, function (action) {
                            allChecked = allChecked && action.selected;
                        });
                        QUnit.ok(allChecked, "All actions are marked as selected in a JS object after selecting corresponding checkboxes");

                        // Reload review step
                        $('#reviewPolicyInfo', editPolView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewPolicyStepTemplate.html', editPolView.data));
                    });

                    $('#reviewPolicyInfo', editPolView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewPolicyStepTemplate.html', editPolView.data, function () {
                        QUnit.ok(editPolView.$el.find('#reviewName').html() === pol.name, "Correct name is displayed in the review step");
                        if ((!editPolView.$el.find('#reviewDesc').html()) && (!pol.description)) {
                            // both are undefined.
                            QUnit.ok(true, "Correct description is displayed in the review step");
                        } else {
                            QUnit.ok(editPolView.$el.find('#reviewDesc').html() === (pol.description ? pol.description : ''), "Correct description is displayed in the review step");
                        }

                        // Resources
                        if (pol.resources.length) {
                            var resources = [];
                            _.each(editPolView.$el.find('ul#reviewRes').find('li'), function (value, key) {
                                resources[key] = value.innerHTML;
                            });

                            QUnit.ok(_.isEqual(resources, pol.resources), "Correct resources are displayed in the review step");
                        }


                        // Actions
                        if (pol.actions.length) {
                            var actions = [],
                                polSelectedActions = _.where(pol.actions, {selected: true}),
                                actionPair;

                            _.each(editPolView.$el.find('#reviewActions').find('li'), function (value) {
                                actionPair = value.innerHTML.split(':');
                                actions.push({action: actionPair[0].trim(), value: actionPair[1].trim() === 'Allowed', selected: true});
                            });

                            QUnit.ok(_.isEqual(actions, polSelectedActions), "Correct actions are displayed in the review step");
                        }

                        /* TODO
                        // Subject Conditions
                        if (pol.subjects.length) {
                            var subjects = [];
                            _.each(editPolView.$el.find('ul#reviewSubjects').find('li'), function (value, key) {
                                subjects[key] = value.innerHTML;
                            });

                            QUnit.ok(_.isEqual(subjects, pol.subjects), "Correct subject conditions are displayed in the review step");
                        }

                        // Environment Conditions
                        if (pol.conditions.length) {
                            var envConditions = [];
                            _.each(editPolView.$el.find('ul#reviewEnvConditions').find('li'), function (value, key) {
                                envConditions[key] = value.innerHTML;
                            });

                            QUnit.ok(_.isEqual(envConditions, pol.conditions), "Correct environment conditions are displayed in the review step");
                        }
                        */


                        QUnit.start();
                    }));

                });
            });

            QUnit.asyncTest("Create new policy", function () {
                editPolView.element = $("<div>")[0];

                editPolView.render(['sunIdentityServerLibertyPPService'], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="" placeholder="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td class="res-name">{{this}}</td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each options.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>'),
                        actionsViewEl = $('<div>').append('<thead><tr class="header-titles"><th><input class="toggle-all-actions" type="checkbox"/></th><th><a href="">Action</a></th><th><a href="">Permission</a></th></tr></thead><tbody>{{#each entity.actions}}<tr><td><input class="toggle-action" type="checkbox"{{#if selected}}checked{{/if}}data-action-name="{{action}}"/></td><td class="action-name">{{action}}</td><td><div class="group-field-row"><input type="radio" name="action{{@index}}" id="allow{{@index}}" value="Allow" data-action-name="{{action}}"{{#if value}}checked{{/if}}/><label for="allow{{@index}}">Allow</label><input type="radio" name="action{{@index}}" id="deny{{@index}}" value="Deny" data-action-name="{{action}}"{{#unless value}}checked{{/unless}}/><label for="deny{{@index}}">Deny</label></div></td></tr>{{/each}}</tbody>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];
                    actionsView.element = actionsViewEl[0];

                    QUnit.ok(editPolView.accordion.getActive() === 0, "First step of accordion is selected");
                    QUnit.ok(editPolView.$el.find('#cancelButton').length, "Cancel button is available");

                    // Step 1
                    QUnit.ok(editPolView.$el.find('#policyName').val() === '', "Name is empty");
                    QUnit.ok(editPolView.$el.find('#description').val() === '', "Description is empty");

                    // Step 2
                    resListView.render([], function () {
                        var resources = resListView.$el.find('.res-name');
                        QUnit.ok(resources.length === 0, "No resources present");
                    });

                    // Step 3
                    actionsView.render([], function () {
                        var availableActions = actionsView.data.options.availableActions,
                            actionsCells = actionsView.$el.find('.action-name');
                        QUnit.ok(availableActions.length === actionsCells.length, "Correct number of actions is displayed");

                        var actionsPresent = true;
                        _.each(actionsCells, function (val, key, list) {
                            actionsPresent = actionsPresent && _.find(availableActions, function (action) {
                                return action.action === val.innerHTML;
                            });
                        });
                        QUnit.ok(actionsPresent, "Actions are displayed correctly (available for selected application type)");
                        QUnit.start();
                    });
                });
            });

            QUnit.asyncTest("List all policies", function () {
                managePolView.element = $("<div>")[0];

                $("#qunit-fixture").append(managePolView.element);

                managePolView.render(['sunIdentityServerLibertyPPService'], function () {
                    var table = $('#managePolicies', managePolView.$el),
                        rowData = table.jqGrid('getRowData'),
                        postedData = table.jqGrid('getGridParam', 'postData'),
                        recordsTotal = table.jqGrid('getGridParam', 'records'),
                        totalNumberOfPages = table.jqGrid('getGridParam', 'lastpage'),
                        recordsPerPage = table.jqGrid('getGridParam', 'rowNum'),
                        rowList = table.jqGrid('getGridParam', 'rowList'),
                        remaining = table.jqGrid('getGridParam', 'userData').remaining;

                    QUnit.ok(rowData.length > 0, "At least one policy listed in the table");
                    QUnit.ok(rowData.length === table.find("tr[id]").length, "Number of rows in grid match number displayed");

                    QUnit.ok(managePolView.$el.find('#backToApps').length, "Back button is available");

                    QUnit.ok(table.jqGrid('getGridParam', 'colNames').length === table.find("tr[id]")[0].children.length,
                        'Total number of columns displayed matches number of columns requested');

                    // sorting
                    QUnit.ok(managePolView.$el.find('#managePolicies_name').find('.s-ico').length === 1,
                        'Sort icon is present for the name column');

                    QUnit.ok(managePolView.$el.find('#managePolicies_name').find('span[sort=desc]').hasClass('ui-state-disabled'),
                        'Name is sorted in ascending order');

                    // Pagination
                    QUnit.ok($('#policiesPager', managePolView.$el).length === 1, 'Pager is present');

                    QUnit.ok(rowData.length + postedData._pagedResultsOffset + remaining === recordsTotal,
                        'Total number of records is calculated correctly');

                    QUnit.ok(table.jqGrid('getGridParam', 'rowNum') >= rowData.length,
                        'Number of rows in grid is less than or equal to number of rows requested');

                    if (recordsTotal > recordsPerPage) {
                        QUnit.ok(totalNumberOfPages === recordsTotal % recordsPerPage === 0 ?
                            recordsTotal / recordsPerPage : Math.floor(recordsTotal / recordsPerPage) + 1,
                            'Total number of pages is calculated correctly');
                    } else {
                        QUnit.ok(totalNumberOfPages === 1,
                            'Total number of pages is calculated correctly');
                    }

                    // Show/hide columns
                    QUnit.ok($('.navtable', managePolView.$el).length === 1, 'Columns Button is available');
                    QUnit.start();
                });
            });


            QUnit.asyncTest("Unauthorized GET Request", function () {
                conf.loggedUser = {"roles": ["ui-admin"]};
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {
                    route: router.configuration.routes.manageApps,
                    callback: function () {
                        sinon.stub(loginHelper, "logout", function () {
                            QUnit.ok(true, "Logout helper method called");
                            QUnit.start();
                        });

                        eventManager.sendEvent(constants.EVENT_UNAUTHORIZED, {error: {type:"GET"} });
                    }
                });
            });

            QUnit.asyncTest("Unauthorized non-GET Request", function () {
                var loginDialog = require("LoginDialog"),
                    loginDialogSpy = sinon.spy(loginDialog, 'render');

                QUnit.ok(!loginDialogSpy.called, "Login Dialog render function has not yet been called");
                conf.loggedUser = {"roles": ["ui-admin"]};
                eventManager.sendEvent(constants.EVENT_UNAUTHORIZED, {error: {type:"POST"} });
                QUnit.ok(conf.loggedUser !== null, "User info should be retained after UNAUTHORIZED POST error");
                QUnit.ok(loginDialogSpy.called, "Login Dialog render function was called");
                QUnit.start();
            });

            QUnit.test("Add/Edit routes with different input", function () {
                QUnit.equal(router.getLink(router.configuration.routes.editApp, [null]), "app/", "Add App - no arguments provided");
                QUnit.equal(router.getLink(router.configuration.routes.editApp, ["calendar"]), "app/calendar", "Edit App with one argument provided");
                QUnit.equal(router.getLink(router.configuration.routes.editApp, ["test spaces"]), "app/test spaces", "Edit App with space in the name");

                QUnit.equal(router.getLink(router.configuration.routes.editPolicy, ["calendar", null]), "app/calendar/policy/", "Add policy with one argument provided");
                QUnit.equal(router.getLink(router.configuration.routes.editPolicy, ["calendar", "testPolicy"]), "app/calendar/policy/testPolicy", "Edit policy with two arguments provided");
            });


        }
    }
});