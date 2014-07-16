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
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/openam/ui/policy/EditApplicationView",
    "org/forgerock/openam/ui/policy/EditPolicyView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ManageApplicationsView",
    "org/forgerock/openam/ui/policy/ManagePoliciesView",
    "org/forgerock/openam/ui/policy/ActionsView",
    "org/forgerock/openam/ui/policy/ReviewInfoView"
], function (uiUtils, policyDelegate, editAppView, editPolView, resListView, addNewResView, manageAppsView, managePolView, actionsView, reviewInfoView) {
    return {
        executeAll: function (server) {

            module('Manage applications page');

            QUnit.asyncTest("Edit Application", function () {
                editAppView.element = $("<div>")[0];

                editAppView.render(['iPlanetAMWebAgentService'], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td class="res-name">{{this}}</td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each entity.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>'),
                        actionsViewEl = $('<div>').append('<thead><tr class="header-titles"><th><input class="toggle-all-actions" type="checkbox"/></th><th><a href="">Action</a></th><th><a href="">Permission</a></th></tr></thead><tbody>{{#each entity.actions}}<tr><td><input class="toggle-action" type="checkbox"{{#if selected}}checked{{/if}}data-action-name="{{action}}"/></td><td><span class="action-name">{{action}}</span></td><td><div class="group-field-row"><input type="radio" name="action{{@index}}" id="allow{{@index}}" value="Allow" data-action-name="{{action}}"{{#if value}}checked{{/if}}/><label for="allow{{@index}}">Allow</label><input type="radio" name="action{{@index}}" id="deny{{@index}}" value="Deny" data-action-name="{{action}}"{{#unless value}}checked{{/unless}}/><label for="deny{{@index}}">Deny</label></div></td></tr>{{/each}}</tbody>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];
                    actionsView.element = actionsViewEl[0];

                    QUnit.start();

                    QUnit.ok(editAppView.accordion.getActive() === 6, "Last step of accordion is selected");

                    var app = editAppView.data.entity;

                    // Step 1
                    QUnit.ok(editAppView.$el.find('#appName').val() === app.name, "Name is set");
                    QUnit.ok(editAppView.$el.find('#appDescription').val() === (app.description ? app.description : ''), "Description is set");
                    QUnit.ok(editAppView.$el.find('#appType').val() === app.applicationType, "Application type is selected");
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
                    actionsView.render([], function () {
                        // Correct actions are displayed for the selected application type
                        var availableActions = actionsView.data.typeActions[actionsView.data.entity.applicationType],
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
                        var toggleAll = actionsView.$el.find('.toggle-all-actions').attr('checked', 'checked').trigger('click'),
                            allChecked = true;
                        _.each(actionsView.data.entity.actions, function (action) {
                            allChecked = allChecked && action.selected;
                        });
                        QUnit.ok(allChecked, "All actions are marked as selected in a JS object after Toggle All checkbox is selected");

                        toggleAll.removeAttr('checked').trigger('click');
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
                        $('#reviewInfo', editAppView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewApplicationStepTemplate.html', editAppView.data));
                    });

                    // Step 4
                    var subjCond = editAppView.$el.find('[name=subjCond]:checked');
                    QUnit.ok(app.subjects.length === subjCond.length, "The number of checked subject conditions is equal to the length of application subject conditions");

                    // Step 5
                    var envCond = editAppView.$el.find('[name=envCond]:checked');
                    QUnit.ok(app.conditions.length === envCond.length, "The number of checked environment conditions is equal to the length of application environment conditions");

                    // Step 6
                    QUnit.ok(editAppView.$el.find('#conflictRule').is(':checked'), "Decision conflict rule radio is checked");
                    QUnit.ok(app.entitlementCombiner === 'DenyOverride', "Decision conflict rule is set");

                    // Step 7
                    $('#reviewInfo', editAppView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewApplicationStepTemplate.html', editAppView.data, function () {
                        QUnit.ok(editAppView.$el.find('#reviewName').html() === app.name, "Correct name is displayed in the review step");
                        QUnit.ok(editAppView.$el.find('#reviewDescr').html() === (app.description ? app.description : ''), "Correct description is displayed in the review step");
                        QUnit.ok(editAppView.$el.find('#reviewType').html() === app.applicationType, "Correct application type is displayed in the review step");
                        QUnit.ok(editAppView.$el.find('#reviewRealm').html() === app.realm, "Correct realm is displayed in the review step");

                        // Resources
                        var resources = _.initial(editAppView.$el.find('#reviewRes').html().split('<br>'));
                        _.each(resources, function (value, key) {
                            resources[key] = value.trim();
                        });
                        QUnit.ok(_.isEqual(resources, app.resources), "Correct resources are displayed in the review step");

                        // Subject Conditions
                        var subjects = _.initial(editAppView.$el.find('#reviewSubj').html().split('<br>'));
                        _.each(subjects, function (value, key) {
                            subjects[key] = value.trim();
                        });
                        QUnit.ok(_.isEqual(subjects, app.subjects), "Correct subject conditions are displayed in the review step");

                        // Actions
                        var actionsDisplayed = _.initial(editAppView.$el.find('#reviewActions').html().split('<br>')),
                            actions = [],
                            actionPair,
                            appSelectedActions = [];
                        _.each(app.actions, function (value) {
                            if (value.selected) {
                                appSelectedActions.push(value);
                            }
                        });
                        _.each(actionsDisplayed, function (value, key) {
                            actionPair = value.split(':');
                            actions.push({action: actionPair[0].trim(), selected: true, value: actionPair[1].trim() === 'Allowed'});
                        });
                        QUnit.ok(_.isEqual(actions, appSelectedActions), "Correct actions are displayed in the review step");

                        // Environment Conditions
                        var envConditions = _.initial(editAppView.$el.find('#reviewEnv').html().split('<br>'));
                        _.each(envConditions, function (value, key) {
                            envConditions[key] = value.trim();
                        });
                        QUnit.ok(_.isEqual(envConditions, app.conditions), "Correct environment conditions are displayed in the review step");

                        // Entitlement Combiner
                        QUnit.ok(editAppView.$el.find('#reviewEntComb').html().split(':')[0] === app.entitlementCombiner, "Correct name is displayed in the review step");
                    }));
                });
            });

            QUnit.asyncTest("Create new application", function () {
                editAppView.element = $("<div>")[0];

                editAppView.render([], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td><span class="res-name">{{this}}</span></td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each entity.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>'),
                        actionsViewEl = $('<div>').append('<thead><tr class="header-titles"><th><input class="toggle-all-actions" type="checkbox"/></th><th><a href="">Action</a></th><th><a href="">Permission</a></th></tr></thead><tbody>{{#each entity.actions}}<tr><td><input class="toggle-action" type="checkbox"{{#if selected}}checked{{/if}}data-action-name="{{action}}"/></td><td class="action-name">{{action}}</td><td><div class="group-field-row"><input type="radio" name="action{{@index}}" id="allow{{@index}}" value="Allow" data-action-name="{{action}}"{{#if value}}checked{{/if}}/><label for="allow{{@index}}">Allow</label><input type="radio" name="action{{@index}}" id="deny{{@index}}" value="Deny" data-action-name="{{action}}"{{#unless value}}checked{{/unless}}/><label for="deny{{@index}}">Deny</label></div></td></tr>{{/each}}</tbody>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];
                    actionsView.element = actionsViewEl[0];

                    QUnit.start();

                    QUnit.ok(editAppView.accordion.getActive() === 0, "First step of accordion is selected");

                    var app = editAppView.data.entity;

                    // Step 1
                    QUnit.ok(editAppView.$el.find('#appName').val() === '', "Name is empty");
                    QUnit.ok(editAppView.$el.find('#appDescription').val() === '', "Description is empty");
                    QUnit.ok(editAppView.$el.find('#appType').val() === app.applicationType, "Application type is selected");
                    QUnit.ok(editAppView.$el.find('#appRealm').val() === '', "Realm is empty");

                    // Step 2
                    resListView.render([], function () {
                        var resources = resListView.$el.find('.res-name');
                        QUnit.ok(resources.length === 0, "No resources present");
                    });

                    // Step 3
                    actionsView.render([], function () {
                        // Correct actions are displayed for the selected application type
                        var availableActions = actionsView.data.typeActions[actionsView.data.entity.applicationType],
                            actionsCells = actionsView.$el.find('.action-name');
                        QUnit.ok(availableActions.length === actionsCells.length, "Correct number of actions is displayed");

                        var actionsPresent = true;
                        _.each(actionsCells, function (val, key, list) {
                            actionsPresent = actionsPresent && _.find(availableActions, function (action) {
                                return action.action === val.innerHTML;
                            });
                        });
                        QUnit.ok(actionsPresent, "Actions are displayed correctly (available for selected application type)");
                    });

                    // Step 4
                    var subjCond = editAppView.$el.find('[name=subjCond]:checked');
                    QUnit.ok(subjCond.length === 0, "0 subject conditions is selected");

                    // Step 5
                    var envCond = editAppView.$el.find('[name=envCond]:checked');
                    QUnit.ok(envCond.length === 0, "0 environment conditions is selected");

                    // Step 6
                    QUnit.ok(editAppView.$el.find('#conflictRule').is(':checked'), "Decision conflict rule radio is checked");
                    QUnit.ok(app.entitlementCombiner === 'DenyOverride', "Decision conflict rule is set");
                });
            });

            QUnit.asyncTest("List all applications", function () {
                manageAppsView.element = $("<div>")[0];

                manageAppsView.render([], function () {
                    QUnit.start();
                    $('#manageApps', manageAppsView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ListApplicationsTemplate.html', manageAppsView.data, function () {
                        var table = $('.filter-sort-grid', manageAppsView.$el),
                            tRows = table.find('tbody tr');

                        QUnit.ok(tRows.length === manageAppsView.data.result.length, "Applications are listed in the table");
                    }));
                });
            });

            module('Manage policies page');

            QUnit.asyncTest("Edit Policy", function () {
                editPolView.element = $("<div>")[0];

                editPolView.render(['im', 'newPolicy'], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td class="res-name">{{this}}</td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each entity.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>'),
                        actionsViewEl = $('<div>').append('<thead><tr class="header-titles"><th><input class="toggle-all-actions" type="checkbox"/></th><th><a href="">Action</a></th><th><a href="">Permission</a></th></tr></thead><tbody>{{#each entity.actions}}<tr><td><input class="toggle-action" type="checkbox"{{#if selected}}checked{{/if}}data-action-name="{{action}}"/></td><td><span class="action-name">{{action}}</span></td><td><div class="group-field-row"><input type="radio" name="action{{@index}}" id="allow{{@index}}" value="Allow" data-action-name="{{action}}"{{#if value}}checked{{/if}}/><label for="allow{{@index}}">Allow</label><input type="radio" name="action{{@index}}" id="deny{{@index}}" value="Deny" data-action-name="{{action}}"{{#unless value}}checked{{/unless}}/><label for="deny{{@index}}">Deny</label></div></td></tr>{{/each}}</tbody>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];
                    actionsView.element = actionsViewEl[0];

                    QUnit.start();

                    QUnit.ok(editPolView.accordion.getActive() === 5, "Last step of accordion is selected");

                    var pol = editPolView.data.entity;

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

                        QUnit.ok(resPattern.find('option').length === pol.resourcePatterns.length, "Correct number of resource patterns");

                        var resPatternPresent = true;
                        _.each(resPattern.find('option'), function (val, key, list) {
                            resPatternPresent = resPatternPresent && _.contains(pol.resourcePatterns, val.innerHTML);
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
                        var availableActions = actionsView.data.entity.availableActions,
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
                        var toggleAll = actionsView.$el.find('.toggle-all-actions').attr('checked', 'checked').trigger('click'),
                            allChecked = true;
                        _.each(actionsView.data.entity.actions, function (action) {
                            allChecked = allChecked && action.selected;
                        });
                        QUnit.ok(allChecked, "All actions are marked as selected in a JS object after Toggle All checkbox is selected");

                        toggleAll.removeAttr('checked').trigger('click');
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

                    // Step 6
                    $('#reviewPolicyInfo', editPolView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ReviewPolicyStepTemplate.html', editPolView.data, function () {
                        QUnit.ok(editPolView.$el.find('#reviewPolName').html() === pol.name, "Correct name is displayed in the review step");
                        QUnit.ok(editPolView.$el.find('#reviewPolDescr').html() === (pol.description ? pol.description : ''), "Correct description is displayed in the review step");

                        // Resources
                        var resources = _.initial(editPolView.$el.find('#reviewPolRes').html().split('<br>'));
                        _.each(resources, function (value, key) {
                            resources[key] = value.trim();
                        });
                        QUnit.ok(_.isEqual(resources, pol.resources), "Correct resources are displayed in the review step");

                        // Actions
                        var actionsDisplayed = _.initial(editPolView.$el.find('#reviewPolActions').html().split('<br>')),
                            actions = [],
                            actionPair,
                            SelectedActions = [];
                        _.each(pol.actions, function (value) {
                            if (value.selected) {
                                SelectedActions.push(value);
                            }
                        });
                        _.each(actionsDisplayed, function (value, key) {
                            actionPair = value.split(':');
                            actions.push({action: actionPair[0].trim(), selected: true, value: actionPair[1].trim() === 'Allowed'});
                        });
                        QUnit.ok(_.isEqual(actions, SelectedActions), "Correct actions are displayed in the review step");
                    }));
                });
            });

            QUnit.asyncTest("Create new policy", function () {
                editPolView.element = $("<div>")[0];

                editPolView.render(['im'], function () {
                    var resListViewEl = $('<div>').append('<table class="filter-sort-grid resources-grid"><thead><tr class="header-actions"><th colspan="3"><input id="deleteResources" type="button" class="button" value="Delete Selected"></th></tr><tr class="header-titles"><th><input class="toggle-all-resources" type="checkbox"/></th><th><a href="">Resource</a></th></tr><tr class="header-filter"><th></th><th><input type="text" value="Filter ..."/></th></tr></thead><tbody>{{#each entity.resources}}<tr><td><input type="checkbox" data-resource-index="{{@index}}"/></td><td><span class="res-name">{{this}}</span></td></tr>{{/each}}</tbody><tfoot></tfoot></table>'),
                        addNewResViewEl = $('<div>').append('<fieldset class="fieldset"><legend>Add New</legend><div class="group-field-block"><label class="prop-name" for="urlResource">New URL resource:</label><select class="prop-val" id="urlResource">{{#each entity.resourcePatterns}}<option value="{{this}}">{{this}}</option>{{/each}}</select></div><div class="group-field-block"><label class="prop-name">URL resource pattern:</label><span class="resource-pattern"><input class="resource-url-part" type="text"/></span></div><input type="button" class="button" value="Add" id="addResource"/></fieldset>'),
                        actionsViewEl = $('<div>').append('<thead><tr class="header-titles"><th><input class="toggle-all-actions" type="checkbox"/></th><th><a href="">Action</a></th><th><a href="">Permission</a></th></tr></thead><tbody>{{#each entity.actions}}<tr><td><input class="toggle-action" type="checkbox"{{#if selected}}checked{{/if}}data-action-name="{{action}}"/></td><td class="action-name">{{action}}</td><td><div class="group-field-row"><input type="radio" name="action{{@index}}" id="allow{{@index}}" value="Allow" data-action-name="{{action}}"{{#if value}}checked{{/if}}/><label for="allow{{@index}}">Allow</label><input type="radio" name="action{{@index}}" id="deny{{@index}}" value="Deny" data-action-name="{{action}}"{{#unless value}}checked{{/unless}}/><label for="deny{{@index}}">Deny</label></div></td></tr>{{/each}}</tbody>');

                    resListView.element = resListViewEl[0];
                    addNewResView.element = addNewResViewEl[0];
                    actionsView.element = actionsViewEl[0];

                    QUnit.start();

                    QUnit.ok(editPolView.accordion.getActive() === 0, "First step of accordion is selected");

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
                        var availableActions = actionsView.data.entity.availableActions,
                            actionsCells = actionsView.$el.find('.action-name');
                        QUnit.ok(availableActions.length === actionsCells.length, "Correct number of actions is displayed");

                        var actionsPresent = true;
                        _.each(actionsCells, function (val, key, list) {
                            actionsPresent = actionsPresent && _.find(availableActions, function (action) {
                                return action.action === val.innerHTML;
                            });
                        });
                        QUnit.ok(actionsPresent, "Actions are displayed correctly (available for selected application type)");
                    });
                });
            });

            QUnit.asyncTest("List all policies", function () {
                managePolView.element = $("<div>")[0];

                managePolView.render(['im'], function () {
                    QUnit.start();
                    $('#managePolicies', managePolView.$el).html(uiUtils.fillTemplateWithData('templates/policy/ListPoliciesTemplate.html', managePolView.data, function () {
                        var table = $('.filter-sort-grid', managePolView.$el),
                            tRows = table.find('tbody tr');

                        QUnit.ok(tRows.length === managePolView.data.result.length, "Applications are listed in the table");
                    }));
                });
            });
        }
    }
});