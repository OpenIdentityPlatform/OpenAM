/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/applications/EditApplicationView",
    "org/forgerock/openam/ui/policy/policies/EditPolicyView",
    "org/forgerock/openam/ui/policy/applications/ManageApplicationsView",
    "org/forgerock/openam/ui/policy/policies/ManagePoliciesView",
    "org/forgerock/openam/ui/policy/common/ActionsView",
    "org/forgerock/openam/ui/policy/policies/attributes/ResponseAttrsStaticView",
    "org/forgerock/openam/ui/policy/policies/attributes/ResponseAttrsUserView",
    "org/forgerock/openam/ui/policy/policies/ResourcesView",
    "org/forgerock/openam/ui/policy/resources/CreatedResourcesView",
    "org/forgerock/openam/ui/policy/common/StripedListView"
], function (eventManager, constants, conf, router, loginHelper, uiUtils, policyDelegate, editAppView, editPolicyView, manageAppsView, policyListView, actionsView, ResponseAttrsStaticView, responseAttrsUserView, policyResourcesView, createdResourcesView, StripedList) {
    return {
        executeAll: function (server) {

            module('Applications');

            QUnit.asyncTest("Edit Application", function () {
                editAppView.element = $("<div>")[0];
                $("#qunit-fixture").append(editAppView.element);

                editAppView.render(['iPlanetAMWebAgentService'], function () {
                    var entity = editAppView.data.entity;

                    QUnit.ok(editAppView.accordion.getActive() === 2, "Last step of accordion is selected");
                    QUnit.ok(editAppView.$el.find('#backButton').length, "Back button is available");

                    QUnit.ok(editAppView.$el.find('#appName').val() === entity.name, "Name is set");
                    QUnit.ok(editAppView.$el.find('#appDescription').val() === (entity.description ? entity.description : ''), "Description is set");

                    // Resource Types
                    var availableNames = _.pluck(editAppView.data.options.availableResourceTypes, 'name'),
                        selected = _.findByValues(editAppView.data.options.allResourceTypes, 'uuid', editAppView.data.entity.resourceTypeUuids);

                    editAppView.resourceTypesListView = new StripedList();
                    editAppView.resourceTypesListView.render({
                        items: availableNames,
                        title: $.t('policy.resourceTypes.availableResourceTypes'),
                        filter: true,
                        clickItem: editAppView.selectResourceType.bind(editAppView)
                    }, '<div></div>', function () {

                        editAppView.resourceTypesListSelectedView = new StripedList();
                        editAppView.resourceTypesListSelectedView.render({
                            items: editAppView.data.options.selectedResourceTypeNames,
                            title: $.t('policy.resourceTypes.selectedResourceTypes'),
                            created: true,
                            clickItem: editAppView.deselectResourceType.bind(editAppView)
                        }, '<div></div>', function () {

                            var leftItems = editAppView.resourceTypesListView.$el.find('.striped-list ul li:not(.invalid)'),
                                rightItems = editAppView.resourceTypesListSelectedView.$el.find('.striped-list ul li:not(.invalid)'),
                                initialRight = rightItems.length;

                            // select and deselect resource types

                            // add 2
                            $(leftItems[0]).trigger('click');
                            $(leftItems[1]).trigger('click');

                            // remove 1
                            $(rightItems[0]).find('.icon-close').trigger('click');

                            QUnit.equal(entity.resourceTypeUuids.length, initialRight + 1, 'Resource Types can be successfully selected and deselected');
                        });
                    });

                    $('#reviewInfo', editAppView.$el).html(uiUtils.fillTemplateWithData('templates/policy/applications/ReviewApplicationStepTemplate.html', editAppView.data, function () {
                        QUnit.ok(editAppView.$el.find('#reviewName').html().trim() === entity.name, "Correct name is displayed in the review step");
                        QUnit.ok(!editAppView.$el.find('#reviewName').parents('.review-row').hasClass('.invalid'), "Validate isn't displayed in the review step");

                        if ((!editAppView.$el.find('#reviewDescr').html()) && (!entity.description)) {
                            // both are undefined.
                            QUnit.ok(true, "Correct description is displayed in the review step");
                        } else {
                            QUnit.ok(editAppView.$el.find('#reviewDesc').html() === (entity.description ? entity.description : ''), "Correct description is displayed in the review step");
                        }

                        QUnit.ok(!editAppView.$el.find('input[name="submitForm"]').is(':disabled'), "Finish button isn't disabled");

                        QUnit.start();
                    }));
                });
            });

            QUnit.asyncTest("Create new application", function () {
                editAppView.element = $("<div>")[0];
                $("#qunit-fixture").append(editAppView.element);

                editAppView.render([], function () {

                    QUnit.ok(editAppView.accordion.getActive() === 0, "First step of accordion is selected");
                    QUnit.ok(editAppView.$el.find('#backButton').length, "Back button is available");

                    QUnit.ok(editAppView.$el.find('#appName').val() === '', "Name is empty");
                    QUnit.ok(editAppView.$el.find('#appDescription').val() === '', "Description is empty");

                    var availableNames = _.pluck(editAppView.data.options.availableResourceTypes, 'name'),
                        selected = _.findByValues(editAppView.data.options.allResourceTypes, 'uuid', editAppView.data.entity.resourceTypeUuids);

                    editAppView.resourceTypesListView = new StripedList();
                    editAppView.resourceTypesListView.render({
                        items: availableNames,
                        title: $.t('policy.resourceTypes.availableResourceTypes'),
                        filter: true,
                        clickItem: editAppView.selectResourceType.bind(editAppView)
                    }, '<div></div>', function () {

                        editAppView.resourceTypesListSelectedView = new StripedList();
                        editAppView.resourceTypesListSelectedView.render({
                            items: editAppView.data.options.selectedResourceTypeNames,
                            title: $.t('policy.resourceTypes.selectedResourceTypes'),
                            created: true,
                            clickItem: editAppView.deselectResourceType.bind(editAppView)
                        }, '<div></div>', function () {

                            var leftItems = editAppView.resourceTypesListView.$el.find('.striped-list ul li:not(.invalid)'),
                                rightItems = editAppView.resourceTypesListSelectedView.$el.find('.striped-list ul li:not(.invalid)');

                            QUnit.equal(rightItems.length, 0, 'No Resource Types are selected');
                            QUnit.equal(editAppView.resourceTypesListSelectedView.$el.find('.striped-list ul li.invalid').length, 1, 'Empty list of Resource Types does not pass validation');

                            $(leftItems[0]).trigger('click');
                            QUnit.equal(editAppView.resourceTypesListSelectedView.$el.find('.striped-list ul li.invalid').length, 0, 'Validation message is not displayed for non-empty list');
                            QUnit.equal(editAppView.data.entity.resourceTypeUuids.length, 1, 'Resource Types can be successfully selected');
                        });
                    });

                    $('#reviewInfo', editAppView.$el).html(uiUtils.fillTemplateWithData('templates/policy/applications/ReviewApplicationStepTemplate.html', editAppView.data, function () {
                        QUnit.ok(editAppView.$el.find('#reviewName').hasClass('invalid'), "Name does not pass validation");
                        QUnit.ok(editAppView.$el.find('input[name="submitForm"]').is(':disabled'), "Finish button is disabled");
                        QUnit.start();
                    }));
                });
            });

            QUnit.asyncTest("List all applications", function () {
                manageAppsView.element = $("<div>")[0];
                $("#qunit-fixture").append(manageAppsView.element);

                manageAppsView.render([], function () {
                    var table = $('#apps', manageAppsView.$el),
                        postedData = table.jqGrid('getGridParam', 'postData'),
                        rowData = table.jqGrid('getRowData'),
                        recordsTotal = table.jqGrid('getGridParam', 'records'),
                        totalNumberOfPages = table.jqGrid('getGridParam', 'lastpage'),
                        recordsPerPage = table.jqGrid('getGridParam', 'rowNum'),
                        rowList = table.jqGrid('getGridParam', 'rowList'),
                        remaining = table.jqGrid('getGridParam', 'userData').remaining;

                    QUnit.ok(conf.globalData.policyEditor, 'Configuration file loaded');

                    QUnit.ok(rowData.length > 0, "At least one application listed in the table");
                    QUnit.ok(rowData.length === table.find("tr[id]").length, "Number of rows in grid match number displayed");

                    QUnit.ok(table.jqGrid('getGridParam', 'colNames').length === table.find("tr[id]")[0].children.length,
                        'Total number of columns displayed matches number of columns requested');

                    // sorting
                    QUnit.ok(manageAppsView.$el.find('#apps_name').find('.s-ico').length === 1,
                        'Sort icon is present for the name column');

                    QUnit.ok(manageAppsView.$el.find('#apps_name').find('span[sort=desc]').hasClass('ui-state-disabled'),
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

            module('Policies');

            QUnit.asyncTest("Edit Policy", function () {
                var appName = 'iPlanetAMWebAgentService',
                    policyName = 'test_pol';

                editPolicyView.element = $("<div>")[0];
                $("#qunit-fixture").append(editPolicyView.element);

                editPolicyView.render([appName, policyName], function () {
                    var entity = editPolicyView.data.entity,
                        options = editPolicyView.data.options;

                    QUnit.ok(editPolicyView.validationFields && editPolicyView.validationFields.length > 0, 'Validation is present');
                    QUnit.equal(editPolicyView.$el.find('[name="submitForm"]').is(':disabled'), false, 'Submit button is enabled');

                    QUnit.equal(editPolicyView.accordion.getActive(), 6, "Last step of accordion is selected");
                    QUnit.equal(editPolicyView.$el.find('#cancelButton').length, 1, "Cancel button is present");

                    QUnit.equal(editPolicyView.$el.find('#policyName').val(), entity.name, "Name is set");
                    QUnit.ok(editPolicyView.$el.find('#description').val() === (entity.description ? entity.description : ''), "Description is set");
                    QUnit.equal(editPolicyView.$el.find('#availableResTypes').val(), entity.resourceTypeUuid, "Resource type is selected");

                    policyResourcesView.render(editPolicyView.data, function () {
                        var listItems = policyResourcesView.$el.find('.created-items ul li'),
                            valid = true;

                        _.each(listItems, function (item) {
                            valid = valid && $(item).text() === item.dataset.resource && _.contains(entity.resources, item.dataset.resource);
                        });

                        QUnit.equal(listItems.length, entity.resources.length, "Correct number of resources are displayed");
                        QUnit.ok(valid, "Resources are displayed correctly");

                        // Available Patterns
                        var element = policyResourcesView.availablePatternsView.$el,
                            patterns = element.find('.striped-list ul li'),
                            pattern = element.find('.striped-list-item:eq(0)'),
                            values = [];

                        _.each(patterns, function (item) {
                            values.push($(item).find('span:first-of-type').text());
                        });

                        QUnit.equal(_.difference(values, options.availablePatterns).length, 0, "All available patterns are displayed correctly");

                        pattern.trigger('click');
                        QUnit.equal(pattern.find('span:first-of-type').text(), options.newPattern, "New pattern selected");

                        createdResourcesView.render(editPolicyView.data, function () {
                            var editing = createdResourcesView.$el.find('.editing'),
                                listItems = createdResourcesView.$el.find('#createdResources ul li:not(.editing)'),
                                plusButton = editing.find('.icon-plus'),
                                resourceLength,
                                values = [],
                                valid = true,
                                NEW_STR = 'newResource',
                                INVALID_STR = 'invalid/Resource';

                            _.each(listItems, function (item) {
                                values.push(item.dataset.resource);
                                if ($(item).text() !== item.dataset.resource) {
                                    valid = false;
                                }
                            });

                            valid = _.difference(values, entity.resources).length === 0 ? valid : false;

                            _.each(listItems, function (item) {
                                valid = valid && $(item).text() === item.dataset.resource && _.contains(entity.resources, item.dataset.resource);
                            });

                            QUnit.ok(valid, "All resources are displayed correctly");

                            QUnit.equal(editing.data().resource, options.newPattern, "Selected pattern displayed correctly");

                            // Testing new resource can be added.
                            editing.find('input')[0].value = NEW_STR;
                            plusButton.trigger('click');

                            // Testing duplication by trying to re-add the previous resource
                            editing.find('input')[0].value = NEW_STR;
                            plusButton.trigger('click');
                            QUnit.equal(entity.resources.length, _.uniq(entity.resources).length, "Duplicate resource not added");

                            // checking for invalid inputs
                            resourceLength = entity.resources.length;
                            editing.find('input')[0].value = INVALID_STR;
                            plusButton.trigger('click');
                            QUnit.equal(entity.resources.length, resourceLength, "Invalid resource not added");

                            // Delete the NEW_RESOURCE
                            resourceLength = entity.resources.length;
                            listItems = createdResourcesView.$el.find('#createdResources ul li:not(.editing)');
                            var lastAddedItem = listItems.eq(listItems.length - 1);

                            lastAddedItem.find('.icon-close').trigger('click');
                            QUnit.ok(entity.resources.length === resourceLength - 1 && !_.contains(entity.resources, lastAddedItem.data().resource), 'Resource deleted');
                        });
                    });

                    $.when(policyDelegate.getAllUserAttributes()).done(function (allUserAttributes) {
                        var staticAttributes = _.where(editPolicyView.data.entity.resourceAttributes, {type: "Static"}),
                            userAttributes = _.where(editPolicyView.data.entity.resourceAttributes, {type: "User"});

                        allUserAttributes = _.sortBy(allUserAttributes.result);

                        editPolicyView.staticAttrsView = new ResponseAttrsStaticView();
                        editPolicyView.staticAttrsView.render(editPolicyView.data.entity, staticAttributes, '#staticAttrs', function () {

                            var editing = editPolicyView.staticAttrsView.$el.find('.editing'),
                                key = editing.find('[data-attr-add-key]'),
                                val = editing.find('[data-attr-add-val]'),
                                addBtn = editing.find('.icon-plus'),
                                deleteBtn,
                                attrsLengthOld = editPolicyView.staticAttrsView.data.items.length;

                            // add new static attribute
                            key.val('testKey');
                            val.val('testVal');
                            addBtn.trigger('click');
                            QUnit.ok(attrsLengthOld + 1 === editPolicyView.staticAttrsView.data.items.length, "Static attribute can be added");

                            editing = editPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-add-key]');
                            val = editing.find('[data-attr-add-val]');
                            addBtn = editing.find('.icon-plus');
                            key.val('testKey');
                            val.val('testVal');
                            addBtn.trigger('click');
                            QUnit.ok(attrsLengthOld + 1 === editPolicyView.staticAttrsView.data.items.length, "Can't add duplicate static attribute");
                            attrsLengthOld++;

                            editing = editPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-add-key]');
                            val = editing.find('[data-attr-add-val]');
                            addBtn = editing.find('.icon-plus');
                            key.val('testKey2');
                            val.val('testVal2');
                            addBtn.trigger('click');

                            // delete static attribute
                            deleteBtn = _.first(editPolicyView.staticAttrsView.$el.find('#attrTypeStatic ul li:first').find('.icon-close'));
                            $(deleteBtn).trigger('click');
                            QUnit.ok(attrsLengthOld === editPolicyView.staticAttrsView.data.items.length, "Static attribute can be deleted");

                            editing = editPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-add-key]');
                            val = editing.find('[data-attr-add-val]');
                            addBtn = editing.find('.icon-plus');
                            key.val('');
                            val.val('incompleteVal');
                            addBtn.trigger('click');

                            QUnit.ok(!_.find(editPolicyView.staticAttrsView.data.items, { propertyName: "incompleteVal" }), "Static attributes with no key can't be added");
                            editing = editPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-add-key]');
                            val = editing.find('[data-attr-add-val]');
                            addBtn = editing.find('.icon-plus');
                            key.val('incompleteKey');
                            val.val('');
                            addBtn.trigger('click');

                            QUnit.ok(!_.find(editPolicyView.staticAttrsView.data.items, { propertyName: "incompleteKey" }), "Static attributes with no value can't be added");
                        });

                        responseAttrsUserView.render([userAttributes, allUserAttributes], function () {
                            QUnit.equal(responseAttrsUserView.$el.find('.selectize-input').find('.item').length, userAttributes.length, 'User attributes are selected correctly');
                        });
                    });

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
                        var toggleAll = actionsView.$el.find('.toggle-all').prop('checked', true),
                            allChecked = true;

                        actionsView.toggleAllActions({target: actionsView.$el.find('.toggle-all')[0]});

                        _.each(actionsView.data.entity.actions, function (action) {
                            allChecked = allChecked && action.selected;
                        });
                        QUnit.ok(allChecked, "All actions are marked as selected in a JS object after Toggle All checkbox is selected");

                        toggleAll.prop('checked', false);
                        actionsView.toggleAllActions({target: actionsView.$el.find('.toggle-all')[0]});

                        allChecked = false;
                        _.each(actionsView.data.entity.actions, function (action) {
                            allChecked = allChecked || action.selected;
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
                        var row = actionsView.$el.find('.striped-list li:first-of-type'),
                            actionName = row.data('action-name');
                        QUnit.ok(!_.findWhere(entity.actions, {action: actionName}).selected, "Action is not selected");
                        row.trigger('click');
                        QUnit.ok(_.findWhere(entity.actions, {action: actionName}).selected, "Action is selected after clicking on row");

                        // Reload review step
                        $('#reviewInfo', editPolicyView.$el).html(uiUtils.fillTemplateWithData('templates/policy/policies/ReviewPolicyStepTemplate.html', editPolicyView.data), function () {
                            // Actions
                            if (entity.actions.length) {
                                var actions = [],
                                    polSelectedActions = _.where(entity.actions, {selected: true}),
                                    actionPair;

                                _.each(editPolicyView.$el.find('#reviewActions').find('li'), function (value) {
                                    actionPair = value.innerHTML.split(':');
                                    actions.push({action: actionPair[0].trim(), value: actionPair[1].trim() === 'Allowed', selected: true});
                                });

                                QUnit.ok(_.isEqual(actions, polSelectedActions), "Correct actions are displayed in the review step");
                            }
                        });
                    });

                    $('#reviewInfo', editPolicyView.$el).html(uiUtils.fillTemplateWithData('templates/policy/policies/ReviewPolicyStepTemplate.html', editPolicyView.data, function () {
                        QUnit.ok(editPolicyView.$el.find('#reviewName').text().trim() === entity.name, "Correct name is displayed in the review step");
                        if ((!editPolicyView.$el.find('#reviewDesc').html()) && (!entity.description)) {
                            // both are undefined.
                            QUnit.ok(true, "Correct description is displayed in the review step");
                        } else {
                            QUnit.ok(editPolicyView.$el.find('#reviewDesc').html() === (entity.description ? entity.description : ''), "Correct description is displayed in the review step");
                        }

                        // Resources
                        if (entity.resources.length) {
                            var resources = [];
                            _.each(editPolicyView.$el.find('ul#reviewRes').find('li'), function (value, key) {
                                resources[key] = value.innerHTML;
                            });

                            QUnit.ok(_.isEqual(resources, entity.resources), "Correct resources are displayed in the review step");
                        }

                        // Subject Conditions
                        QUnit.equal(editPolicyView.$el.find('#subjectContainer').find('#addCondition').length, 1, 'Add subject button is present');
                        QUnit.equal(editPolicyView.$el.find('#subjectContainer').find('#addOperator').length, 1, 'Add operator button is present');
                        QUnit.ok(editPolicyView.$el.find('#subjectContainer').find('#pickUpItem').empty(), 'Pick up item is empty');
                        QUnit.ok(editPolicyView.$el.find('#subjectContainer').find('#clear').hasClass('inactive'), 'Clear button is disabled');

                        editPolicyView.$el.find('#subjectContainer').find('#addCondition').trigger('click');
                        QUnit.ok(editPolicyView.$el.find('#subjectContainer').find('#pickUpItem').children().length > 0, 'Pick up item is not empty');
                        QUnit.ok(!editPolicyView.$el.find('#subjectContainer').find('#clear').hasClass('inactive'), 'Clear button is enabled');

                        if (entity.subject) {
                            var reviewSubj = JSON.parse(editPolicyView.$el.find('#reviewSubj').text());
                            QUnit.ok(_.isEqual(reviewSubj, entity.subject), "Correct environment conditions are displayed in the review step");
                        }

                        // Environment Conditions
                        QUnit.equal(editPolicyView.$el.find('#environmentContainer').find('#addCondition').length, 1, 'Add condition button is present');
                        QUnit.equal(editPolicyView.$el.find('#environmentContainer').find('#addOperator').length, 1, 'Add operator button is present');
                        QUnit.ok(editPolicyView.$el.find('#environmentContainer').find('#pickUpItem').empty(), 'Pick up item is empty');
                        QUnit.ok(editPolicyView.$el.find('#environmentContainer').find('#clear').hasClass('inactive'), 'Clear button is disabled');

                        editPolicyView.$el.find('#environmentContainer').find('#addCondition').trigger('click');
                        QUnit.ok(editPolicyView.$el.find('#environmentContainer').find('#pickUpItem').children().length > 0, 'Pick up item is not empty');
                        QUnit.ok(!editPolicyView.$el.find('#environmentContainer').find('#clear').hasClass('inactive'), 'Clear button is enabled');

                        if (entity.condition) {
                            var reviewEnv = JSON.parse(editPolicyView.$el.find('#reviewEnv').text());
                            QUnit.ok(_.isEqual(reviewEnv, entity.condition), "Correct environment conditions are displayed in the review step");
                        }

                        QUnit.start();
                    }));
                });
            });

            QUnit.asyncTest("Create new policy", function () {
                editPolicyView.element = $("<div>")[0];
                $("#qunit-fixture").append(editPolicyView.element);

                editPolicyView.render(['iPlanetAMWebAgentService'], function () {
                    QUnit.ok(editPolicyView.accordion.getActive() === 0, "First step of accordion is selected");
                    QUnit.ok(editPolicyView.$el.find('#cancelButton').length, "Cancel button is available");

                    QUnit.ok(editPolicyView.$el.find('#policyName').val() === '', "Name is empty");
                    QUnit.ok(editPolicyView.$el.find('#description').val() === '', "Description is empty");

                    createdResourcesView.render(editPolicyView.data, function () {
                        var resources = createdResourcesView.$el.find('.res-name');
                        QUnit.ok(resources.length === 0, "No resources present");
                    });

                    actionsView.render(editPolicyView.data, function () {
                        QUnit.equal(actionsView.$el.find('.striped-list li.invalid').length, 1, 'No actions are available for selection as resource type is not selected');
                    });

                    $('#reviewInfo', editPolicyView.$el).html(uiUtils.fillTemplateWithData('templates/policy/policies/ReviewPolicyStepTemplate.html', editPolicyView.data, function () {
                        QUnit.ok(editPolicyView.$el.find('#reviewName').hasClass('invalid'), 'Name field is marked as invalid');
                        QUnit.equal(editPolicyView.$el.find('#reviewRes').length, 0, 'Resources field is not displayed in review step as it is invalid');
                        QUnit.ok(editPolicyView.$el.find('[name="submitForm"]').is(':disabled'), 'Submit button is disabled');

                        QUnit.start();
                    }));
                });
            });

            QUnit.asyncTest("List policies", function () {
                var appName = 'iPlanetAMWebAgentService';

                policyListView.element = $("<div>")[0];
                $("#qunit-fixture").append(policyListView.element);

                policyListView.render([appName], function () {
                    var table = $('#policies', policyListView.$el),
                        rowData = table.jqGrid('getRowData'),
                        postedData = table.jqGrid('getGridParam', 'postData'),
                        recordsTotal = table.jqGrid('getGridParam', 'records'),
                        totalNumberOfPages = table.jqGrid('getGridParam', 'lastpage'),
                        recordsPerPage = table.jqGrid('getGridParam', 'rowNum'),
                        rowList = table.jqGrid('getGridParam', 'rowList'),
                        remaining = table.jqGrid('getGridParam', 'userData').remaining;

                    QUnit.equal(policyListView.$el.find('.ui-jqgrid').length, 1, 'Table is rendered');
                    QUnit.equal(policyListView.$el.find('.global-actions').length, 1, 'Table actions are rendered');

                    QUnit.equal(table.find("tr[id]").length, rowData.length, "Number of rows in grid match number displayed");

                    // assuming application property "editable" is true
                    QUnit.equal(policyListView.$el.find('#appNameHeader').find('a').text(), appName, 'App name is displayed correctly');
                    QUnit.equal(policyListView.$el.find('#appNameHeader').find('a').attr('href'), '#app/' + appName, 'App link is correct');

                    QUnit.equal(policyListView.$el.find('#backToApps').length, 1, "Back button is available");
                    QUnit.equal(policyListView.$el.find('.navtable').length, 1, 'Columns Button is available');

                    QUnit.ok(policyListView.$el.find('#policies_name').find('span[sort=desc]').hasClass('ui-state-disabled'), 'Name is sorted in ascending order');

                    QUnit.equal(policyListView.$el.find('#policiesPager').length, 1, 'Pager is present');

                    QUnit.equal(policyListView.$el.find('#policiesPager_left').find('.ui-icon-add').length, 1, 'Columns icon is present');
                    QUnit.equal(policyListView.$el.find('#policiesPager_left').find('.ui-icon-search').length, 1, 'Advanced search icon is present');

                    QUnit.ok(rowData.length + postedData._pagedResultsOffset + remaining === recordsTotal, 'Total number of records is calculated correctly');

                    QUnit.ok(table.jqGrid('getGridParam', 'rowNum') >= rowData.length, 'Number of rows in grid is less than or equal to number of rows requested');

                    if (recordsTotal > recordsPerPage) {
                        QUnit.ok(totalNumberOfPages === recordsTotal % recordsPerPage === 0 ? recordsTotal / recordsPerPage : Math.floor(recordsTotal / recordsPerPage) + 1, 'Total number of pages is calculated correctly');
                    } else {
                        QUnit.equal(totalNumberOfPages, 1, 'Total number of pages is calculated correctly');
                    }

                    QUnit.start();
                });
            });

            module('Common');

            QUnit.asyncTest("Unauthorized GET Request", function () {
                var viewManager = require('org/forgerock/commons/ui/common/main/ViewManager');
                conf.loggedUser = {"roles": ["ui-admin"]};
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {
                    route: router.configuration.routes.manageApps,
                    callback: function () {
                        sinon.stub(viewManager, 'showDialog', function () {
                            QUnit.ok(true, "Login dialog is shown");
                            QUnit.start();
                            delete conf.globalData.authorizationFailurePending;
                            viewManager.showDialog.restore();
                        });

                        eventManager.sendEvent(constants.EVENT_UNAUTHORIZED, {error: {type: "GET"} });
                    }
                });
            });

            QUnit.asyncTest("Unauthorized POST Request", function () {
                var viewManager = require('org/forgerock/commons/ui/common/main/ViewManager');

                sinon.stub(viewManager, 'showDialog', function () {
                    QUnit.ok(true, "Login dialog is shown");
                    QUnit.start();
                    delete conf.globalData.authorizationFailurePending;
                    viewManager.showDialog.restore();
                });

                QUnit.ok(!viewManager.showDialog.called, "Login Dialog render function has not yet been called");
                conf.loggedUser = {"roles": ["ui-admin"]};
                eventManager.sendEvent(constants.EVENT_UNAUTHORIZED, {error: {type: "POST"} });
                QUnit.ok(conf.loggedUser !== null, "User info should be retained after UNAUTHORIZED POST error");
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