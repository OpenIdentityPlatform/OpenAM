/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/policy/login/LoginHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/applications/EditApplicationView",
    "org/forgerock/openam/ui/policy/policies/EditPolicyView",
    "org/forgerock/openam/ui/policy/applications/ApplicationsListView",
    "org/forgerock/openam/ui/policy/policies/PoliciesListView",
    "org/forgerock/openam/ui/policy/policies/PolicyActionsView",
    "org/forgerock/openam/ui/policy/policies/attributes/StaticResponseAttributesView",
    "org/forgerock/openam/ui/policy/policies/attributes/SubjectResponseAttributesView",
    "org/forgerock/openam/ui/policy/policies/ResourcesView",
    "org/forgerock/openam/ui/policy/resources/CreatedResourcesView",
    "org/forgerock/openam/ui/policy/common/StripedListView",
    "org/forgerock/openam/ui/policy/resourcetypes/EditResourceTypeView",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypePatternsView",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypeActionsView",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypesListView",
    "org/forgerock/openam/ui/policy/common/ReviewInfoView"
], function (EventManager, Constants, RealmHelper, Configuration, Router, LoginHelper, UIUtils, PolicyDelegate, EditAppView, EditPolicyView, ApplicationsListView, PolicyListView, PolicyActionsView, StaticResponseAttributesView, SubjectResponseAttributesView, PolicyResourcesView, CreatedResourcesView,
             StripedList, EditResourceTypeView, ResTypePatternsView, ResTypeActionsView, ResTypesListView, ReviewInfoView) {
    return {
        executeAll: function () {

            module('Applications');

            QUnit.asyncTest("Edit Application", function () {
                EditAppView.element = $("<div>")[0];
                $("#qunit-fixture").append(EditAppView.element);

                EditAppView.render(['iPlanetAMWebAgentService'], function () {
                    var entity = EditAppView.data.entity,
                        activeTabIndex = EditAppView.$el.find('#accordion .panel-collapse.in').parent().index();

                    if (activeTabIndex === -1) {
                        activeTabIndex = EditAppView.$el.find('#accordion .panel-collapse.collapsing').parent().index();
                    }
                    QUnit.ok(activeTabIndex === 2, "Last step of accordion is selected");
                    QUnit.ok(EditAppView.$el.find('h1 > a.btn-default').length, "Back button is available");

                    QUnit.ok(EditAppView.$el.find('#appName').val() === entity.name, "Name is set");
                    QUnit.ok(EditAppView.$el.find('#appDescription').val() === (entity.description ? entity.description : ''), "Description is set");

                    // Resource Types
                    var availableNames = _.pluck(EditAppView.data.options.availableResourceTypes, 'name'),
                        selected = _.findByValues(EditAppView.data.options.allResourceTypes, 'uuid', EditAppView.data.entity.resourceTypeUuids);

                    EditAppView.resourceTypesListView = new StripedList();
                    EditAppView.resourceTypesListView.render({
                        items: availableNames,
                        title: $.t('policy.resourceTypes.availableResourceTypes'),
                        filter: true,
                        clickItem: EditAppView.selectResourceType.bind(EditAppView)
                    }, '<div></div>', function () {

                        EditAppView.resourceTypesListSelectedView = new StripedList();
                        EditAppView.resourceTypesListSelectedView.render({
                            items: EditAppView.data.options.selectedResourceTypeNames,
                            title: $.t('policy.resourceTypes.selectedResourceTypes'),
                            created: true,
                            clickItem: EditAppView.deselectResourceType.bind(EditAppView)
                        }, '<div></div>', function () {

                            var leftItems = EditAppView.resourceTypesListView.$el.find('.list-table ul li:not(.text-danger)'),
                                rightItems = EditAppView.resourceTypesListSelectedView.$el.find('.list-table ul li:not(.text-danger)'),
                                initialRight = rightItems.length;

                            // select and deselect resource types
                            // add 2
                            $(leftItems[0]).trigger('click');
                            $(leftItems[1]).trigger('click');

                            // remove 1
                            $(rightItems[0]).find('.icon-close').trigger('click');

                            QUnit.equal(entity.resourceTypeUuids.length, initialRight + 1, 'Resource Types can be successfully selected and deselected');

                            ReviewInfoView.render(EditAppView.data, function () {
                                QUnit.ok(EditAppView.$el.find('#reviewName').html().trim() === entity.name, "Correct name is displayed in the review step");
                                QUnit.ok(!EditAppView.$el.find('#reviewName').parents('.review-row').hasClass('.invalid'), "Validate isn't displayed in the review step");

                                if ((!EditAppView.$el.find('#reviewDescr').html()) && (!entity.description)) {
                                    // both are undefined.
                                    QUnit.ok(true, "Correct description is displayed in the review step");
                                } else {
                                    QUnit.ok(EditAppView.$el.find('#reviewDesc').html().trim() === (entity.description ? entity.description : ''), "Correct description is displayed in the review step");
                                }

                                QUnit.ok(!EditAppView.$el.find('input[name="submitForm"]').is(':disabled'), "Finish button isn't disabled");

                                QUnit.start();
                            }, EditAppView.$el.find('#reviewInfo'), "templates/policy/applications/ReviewApplicationStepTemplate.html");

                        });
                    });
                });
            });

            QUnit.asyncTest("Create new application", function () {
                EditAppView.element = $("<div>")[0];
                $("#qunit-fixture").append(EditAppView.element);

                EditAppView.render([], function () {
                    var activeTab = EditAppView.$el.find('#accordion .panel-collapse.in');

                    if (!activeTab.length) {
                        activeTab = EditAppView.$el.find('#accordion .panel-collapse.collapsing');
                    }
                    QUnit.ok(activeTab.parent().index() === 0, "First step of accordion is selected");
                    QUnit.ok(EditAppView.$el.find('h1 > a.btn-default').length, "Back button is available");

                    QUnit.ok(EditAppView.$el.find('#appName').val() === '', "Name is empty");
                    QUnit.ok(EditAppView.$el.find('#appDescription').val() === '', "Description is empty");

                    var availableNames = _.pluck(EditAppView.data.options.availableResourceTypes, 'name'),
                        selected = _.findByValues(EditAppView.data.options.allResourceTypes, 'uuid', EditAppView.data.entity.resourceTypeUuids);

                    EditAppView.resourceTypesListView = new StripedList();
                    EditAppView.resourceTypesListView.render({
                        items: availableNames,
                        title: $.t('policy.resourceTypes.availableResourceTypes'),
                        filter: true,
                        clickItem: EditAppView.selectResourceType.bind(EditAppView)
                    }, '<div></div>', function () {

                        EditAppView.resourceTypesListSelectedView = new StripedList();
                        EditAppView.resourceTypesListSelectedView.render({
                            items: EditAppView.data.options.selectedResourceTypeNames,
                            title: $.t('policy.resourceTypes.selectedResourceTypes'),
                            created: true,
                            clickItem: EditAppView.deselectResourceType.bind(EditAppView)
                        }, '<div></div>', function () {

                            var leftItems = EditAppView.resourceTypesListView.$el.find('.list-table ul li:not(.text-danger)'),
                                rightItems = EditAppView.resourceTypesListSelectedView.$el.find('.list-table ul li:not(.text-danger)');

                            QUnit.equal(rightItems.length, 0, 'No Resource Types are selected');
                            QUnit.equal(EditAppView.resourceTypesListSelectedView.$el.find('.list-table ul li.text-danger').length, 1, 'Empty list of Resource Types does not pass validation');

                            $(leftItems[0]).trigger('click');
                            QUnit.equal(EditAppView.resourceTypesListSelectedView.$el.find('.list-table ul li.text-danger').length, 0, 'Validation message is not displayed for non-empty list');
                            QUnit.equal(EditAppView.data.entity.resourceTypeUuids.length, 1, 'Resource Types can be successfully selected');

                            ReviewInfoView.render(EditAppView.data, function () {
                                QUnit.ok(EditAppView.$el.find('#reviewName').hasClass('text-danger'), "Name does not pass validation");
                                QUnit.ok(EditAppView.$el.find('input[name="submitForm"]').is(':disabled'), "Finish button is disabled");
                                QUnit.start();
                            }, EditAppView.$el.find('#reviewInfo'), "templates/policy/applications/ReviewApplicationStepTemplate.html");
                        });
                    });
                });
            });

            QUnit.asyncTest("List all applications", function () {
                ApplicationsListView.element = $("<div>")[0];
                $("#qunit-fixture").append(ApplicationsListView.element);

                ApplicationsListView.render([], function () {
                    var table = $('#apps', ApplicationsListView.$el),
                        postedData = table.jqGrid('getGridParam', 'postData'),
                        rowData = table.jqGrid('getRowData'),
                        recordsTotal = table.jqGrid('getGridParam', 'records'),
                        totalNumberOfPages = table.jqGrid('getGridParam', 'lastpage'),
                        recordsPerPage = table.jqGrid('getGridParam', 'rowNum'),
                        rowList = table.jqGrid('getGridParam', 'rowList'),
                        remaining = table.jqGrid('getGridParam', 'userData').remaining;

                    QUnit.ok(Configuration.globalData.policyEditor, 'Configuration file loaded');

                    QUnit.ok(rowData.length > 0, "At least one application listed in the table");
                    QUnit.ok(rowData.length === table.find("tr[id]").length, "Number of rows in grid match number displayed");

                    QUnit.ok(table.jqGrid('getGridParam', 'colNames').length === table.find("tr[id]")[0].children.length,
                        'Total number of columns displayed matches number of columns requested');

                    // sorting
                    QUnit.ok(ApplicationsListView.$el.find('#apps_name').find('.s-ico').length === 1,
                        'Sort icon is present for the name column');

                    QUnit.ok(ApplicationsListView.$el.find('#apps_name').find('span[sort=desc]').hasClass('ui-state-disabled'),
                        'Name is sorted in ascending order');

                    // Pagination
                    QUnit.ok($('#appsPager', ApplicationsListView.$el).length === 1, 'Pager is present');

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
                    QUnit.ok($('.navtable', ApplicationsListView.$el).length === 1, 'Columns Button is available');
                    QUnit.start();
                });
            });

            module('Policies');

            QUnit.asyncTest("Edit Policy", function () {
                var appName = 'iPlanetAMWebAgentService',
                    policyName = 'test_pol';

                EditPolicyView.element = $("<div>")[0];
                $("#qunit-fixture").append(EditPolicyView.element);

                EditPolicyView.render([appName, policyName], function () {
                    var entity = EditPolicyView.data.entity,
                        options = EditPolicyView.data.options,
                        activeTab = EditPolicyView.$el.find('#accordion .panel-collapse.in');

                    if (!activeTab.length) {
                        activeTab = EditPolicyView.$el.find('#accordion .panel-collapse.collapsing');
                    }

                    QUnit.ok(EditPolicyView.validationFields && EditPolicyView.validationFields.length > 0, 'Validation is present');
                    QUnit.equal(EditPolicyView.$el.find('[name="submitForm"]').is(':disabled'), false, 'Submit button is enabled');

                    QUnit.equal(activeTab.parent().index(), 6, "Last step of accordion is selected");
                    QUnit.equal(EditPolicyView.$el.find('h1 > a.btn-default').length, 1, "Cancel button is present");

                    QUnit.equal(EditPolicyView.$el.find('#policyName').val(), entity.name, "Name is set");
                    QUnit.ok(EditPolicyView.$el.find('#description').val() === (entity.description ? entity.description : ''), "Description is set");
                    QUnit.equal(EditPolicyView.$el.find('#availableResTypes').val(), entity.resourceTypeUuid, "Resource type is selected");

                    PolicyResourcesView.render(EditPolicyView.data, function () {
                        var listItems = PolicyResourcesView.$el.find('.created-items ul li'),
                            valid = true;

                        _.each(listItems, function (item) {
                            valid = valid && $(item).text().trim() === item.dataset.resource && _.contains(entity.resources, item.dataset.resource);
                        });

                        QUnit.equal(listItems.length, entity.resources.length, "Correct number of resources are displayed");
                        QUnit.ok(valid, "Resources are displayed correctly");

                        // Available Patterns
                        var element = PolicyResourcesView.availablePatternsView.$el,
                            patterns = element.find('.list-table ul li'),
                            pattern = element.find('.list-group-item:eq(0)'),
                            values = [];

                        _.each(patterns, function (item) {
                            values.push($(item).find('span:first-of-type').text());
                        });

                        QUnit.equal(_.difference(values, options.availablePatterns).length, 0, "All available patterns are displayed correctly");

                        pattern.trigger('click');
                        QUnit.equal(pattern.find('span:first-of-type').text(), options.newPattern, "New pattern selected");

                        CreatedResourcesView.render(EditPolicyView.data, function () {
                            var editing = CreatedResourcesView.$el.find('.editing'),
                                listItems = CreatedResourcesView.$el.find('#createdResources ul li:not(.editing)'),
                                plusButton = editing.find('.icon-plus'),
                                resourceLength,
                                values = [],
                                valid = true,
                                NEW_STR = 'newResource',
                                INVALID_STR = 'invalid/Resource';

                            _.each(listItems, function (item) {
                                values.push(item.dataset.resource);
                                if ($(item).text().trim() !== item.dataset.resource) {
                                    valid = false;
                                }
                            });

                            valid = _.difference(values, entity.resources).length === 0 ? valid : false;

                            _.each(listItems, function (item) {
                                valid = valid && $(item).text().trim() === item.dataset.resource && _.contains(entity.resources, item.dataset.resource);
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
                            listItems = CreatedResourcesView.$el.find('#createdResources ul li:not(.editing)');
                            var lastAddedItem = listItems.eq(listItems.length - 1);

                            lastAddedItem.find('.fa-close').trigger('click');
                            QUnit.ok(entity.resources.length === resourceLength - 1 && !_.contains(entity.resources, lastAddedItem.data().resource), 'Resource deleted');
                        });
                    });

                    $.when(PolicyDelegate.getAllUserAttributes()).done(function (allUserAttributes) {
                        var staticAttributes = _.where(EditPolicyView.data.entity.resourceAttributes, {type: "Static"}),
                            userAttributes = _.where(EditPolicyView.data.entity.resourceAttributes, {type: "User"});

                        allUserAttributes = _.sortBy(allUserAttributes.result);

                        // workaround for delete test. staticAttributes must have one item
                        if (staticAttributes.length == 0) {
                            staticAttributes.push({ "type": "Static", "propertyName": "test", "propertyValues": ["test"] });
                        }

                        EditPolicyView.staticAttrsView = new StaticResponseAttributesView();
                        EditPolicyView.staticAttrsView.render(EditPolicyView.data.entity, staticAttributes, '#staticAttrs', function () {

                            var editing = EditPolicyView.staticAttrsView.$el.find('.editing'),
                                key = editing.find('[data-attr-key]'),
                                val = editing.find('[data-attr-val]'),
                                addBtn = editing.find('.fa-plus'),
                                deleteBtn,
                                attrsLengthOld = EditPolicyView.staticAttrsView.data.items.length;

                            // add new static attribute
                            key.val('testKey');
                            val.val('testVal');
                            addBtn.trigger('click');
                            QUnit.ok(attrsLengthOld + 1 === EditPolicyView.staticAttrsView.data.items.length, "Static attribute can be added");

                            editing = EditPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-key]');
                            val = editing.find('[data-attr-val]');
                            addBtn = editing.find('.fa-plus');
                            key.val('testKey');
                            val.val('testVal');
                            addBtn.trigger('click');
                            QUnit.ok(attrsLengthOld + 1 === EditPolicyView.staticAttrsView.data.items.length, "Can't add duplicate static attribute");
                            attrsLengthOld++;

                            editing = EditPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-key]');
                            val = editing.find('[data-attr-val]');
                            addBtn = editing.find('.fa-plus');
                            key.val('testKey2');
                            val.val('testVal2');
                            addBtn.trigger('click');

                            // delete static attribute
                            deleteBtn = _.first(EditPolicyView.staticAttrsView.$el.find('ul li:first').find('.fa-close'));
                            $(deleteBtn).trigger('click');
                            QUnit.ok(attrsLengthOld === EditPolicyView.staticAttrsView.data.items.length, "Static attribute can be deleted");

                            editing = EditPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-key]');
                            val = editing.find('[data-attr-val]');
                            addBtn = editing.find('.fa-plus');
                            key.val('');
                            val.val('incompleteVal');
                            addBtn.trigger('click');

                            QUnit.ok(!_.find(EditPolicyView.staticAttrsView.data.items, { propertyName: "incompleteVal" }), "Static attributes with no key can't be added");
                            editing = EditPolicyView.staticAttrsView.$el.find('.editing');
                            key = editing.find('[data-attr-key]');
                            val = editing.find('[data-attr-val]');
                            addBtn = editing.find('.fa-plus');
                            key.val('incompleteKey');
                            val.val('');
                            addBtn.trigger('click');

                            QUnit.ok(!_.find(EditPolicyView.staticAttrsView.data.items, { propertyName: "incompleteKey" }), "Static attributes with no value can't be added");
                        });

                        SubjectResponseAttributesView.render([userAttributes, allUserAttributes], function () {
                            QUnit.equal(SubjectResponseAttributesView.$el.find('.selectize-input').find('.item').length, userAttributes.length, 'User attributes are selected correctly');
                        });
                    });

                    PolicyActionsView.render(EditPolicyView.data, function () {
                        // Correct available actions are displayed
                        var availableActions = PolicyActionsView.data.options.availableActions,
                            actionsItems = PolicyActionsView.$el.find('[data-list-item]');
                        QUnit.ok(availableActions.length === actionsItems.length, "Correct number of actions is displayed");

                        var actionsPresent = true;
                        _.each(actionsItems, function (val, key, list) {
                            actionsPresent = actionsPresent && _.find(availableActions, function (action) {
                                return action.action === $(val).text().trim();
                            });
                        });
                        QUnit.ok(actionsPresent, "Actions are displayed correctly (available for selected application type)");

                        // Fixme: toggleAll is impossible now
                        // Toggle all actions
//                        var toggleAll = PolicyActionsView.$el.find('.toggle-all').prop('checked', true),
//                            allChecked = true;
//
//                        PolicyActionsView.toggleAllActions({target: PolicyActionsView.$el.find('.toggle-all')[0]});
//
//                        _.each(PolicyActionsView.data.entity.actions, function (action) {
//                            allChecked = allChecked && action.selected;
//                        });
//                        QUnit.ok(allChecked, "All actions are marked as selected in a JS object after Toggle All checkbox is selected");
//
//
//                        toggleAll.prop('checked', false);
//                        PolicyActionsView.toggleAllActions({target: PolicyActionsView.$el.find('.toggle-all')[0]});
//
//                        allChecked = false;
//                        _.each(PolicyActionsView.data.entity.actions, function (action) {
//                            allChecked = allChecked || action.selected;
//                        });
//                        QUnit.ok(!allChecked, "All actions are marked as deselected in a JS object after Toggle All checkbox is deselected");

                        // Action permissions
                        var permissions = PolicyActionsView.$el.find('input[type=radio][data-action-name]:checked'),
                            correctPermissions = true;
                        _.each(permissions, function (val, key, list) {
                            correctPermissions = correctPermissions &&
                                _.find(PolicyActionsView.data.entity.actions, function (action) {
                                    return action.action === val.getAttribute('data-action-name') &&
                                        action.value === (val.value === 'Allow');
                                });
                        });
                        QUnit.ok(correctPermissions, "All permissions are selected correctly");

                        // Selecting individual actions
                        var row = PolicyActionsView.$el.find('#availableActions .list-group li:first-of-type');
                        QUnit.ok(PolicyActionsView.$el.find('#selectedActions .list-group li:first-of-type').hasClass('text-info'), "Action is not selected");
                        row.trigger('click');
                        QUnit.ok(PolicyActionsView.$el.find('#selectedActions .list-group li[data-list-item]'), "Action is selected after clicking on row");

                        // Reload review step
                        $('#reviewInfo', EditPolicyView.$el).html(UIUtils.fillTemplateWithData('templates/policy/policies/ReviewPolicyStepTemplate.html', EditPolicyView.data), function () {
                            // Actions
                            if (entity.actions.length) {
                                var actions = [],
                                    polSelectedActions = _.where(entity.actions, {selected: true}),
                                    actionPair;

                                _.each(EditPolicyView.$el.find('#reviewActions').find('li'), function (value) {
                                    actionPair = value.innerHTML.split(':');
                                    actions.push({action: actionPair[0].trim(), value: actionPair[1].trim() === 'Allowed', selected: true});
                                });

                                QUnit.ok(_.isEqual(actions, polSelectedActions), "Correct actions are displayed in the review step");
                            }
                        });
                    });

                    ReviewInfoView.render(EditPolicyView.data, function () {
                        QUnit.ok(EditPolicyView.$el.find('#reviewName').text().trim() === entity.name, "Correct name is displayed in the review step");
                        if ((!EditPolicyView.$el.find('#reviewDesc').html()) && (!entity.description)) {
                            // both are undefined.
                            QUnit.ok(true, "Correct description is displayed in the review step");
                        } else {
                            QUnit.ok(EditPolicyView.$el.find('#reviewDesc').html().trim() === (entity.description ? entity.description : ''), "Correct description is displayed in the review step");
                        }

                        // Resources
                        if (entity.resources.length) {
                            var resources = [];
                            _.each(EditPolicyView.$el.find('ul#reviewRes').find('li'), function (value, key) {
                                resources[key] = value.innerHTML;
                            });

                            QUnit.ok(_.isEqual(resources, entity.resources), "Correct resources are displayed in the review step");
                        }

                        // Subject Conditions
                        QUnit.equal(EditPolicyView.$el.find('#subjectContainer').find('#addCondition').length, 1, 'Add subject button is present');
                        QUnit.equal(EditPolicyView.$el.find('#subjectContainer').find('#addOperator').length, 1, 'Add operator button is present');
                        QUnit.ok(EditPolicyView.$el.find('#subjectContainer').find('#pickUpItem').empty(), 'Pick up item is empty');
                        QUnit.ok(EditPolicyView.$el.find('#subjectContainer').find('#clear').hasClass('disabled'), 'Clear button is disabled');

                        EditPolicyView.$el.find('#subjectContainer').find('#addCondition').trigger('click');
                        QUnit.ok(EditPolicyView.$el.find('#subjectContainer').find('#pickUpItem').children().length > 0, 'Pick up item is not empty');
                        QUnit.ok(!EditPolicyView.$el.find('#subjectContainer').find('#clear').hasClass('inactive'), 'Clear button is enabled');

                        if (entity.subject) {
                            var reviewSubj = JSON.parse(EditPolicyView.$el.find('#reviewSubj').text());
                            QUnit.ok(_.isEqual(reviewSubj, entity.subject), "Correct environment conditions are displayed in the review step");
                        }

                        // Environment Conditions
                        QUnit.equal(EditPolicyView.$el.find('#environmentContainer').find('#addCondition').length, 1, 'Add condition button is present');
                        QUnit.equal(EditPolicyView.$el.find('#environmentContainer').find('#addOperator').length, 1, 'Add operator button is present');
                        QUnit.ok(EditPolicyView.$el.find('#environmentContainer').find('#pickUpItem').empty(), 'Pick up item is empty');
                        QUnit.ok(EditPolicyView.$el.find('#environmentContainer').find('#clear').hasClass('disabled'), 'Clear button is disabled');

                        EditPolicyView.$el.find('#environmentContainer').find('#addCondition').trigger('click');
                        QUnit.ok(EditPolicyView.$el.find('#environmentContainer').find('#pickUpItem').children().length > 0, 'Pick up item is not empty');
                        QUnit.ok(!EditPolicyView.$el.find('#environmentContainer').find('#clear').hasClass('inactive'), 'Clear button is enabled');

                        if (entity.condition) {
                            var reviewEnv = JSON.parse(EditPolicyView.$el.find('#reviewEnv').text());
                            QUnit.ok(_.isEqual(reviewEnv, entity.condition), "Correct environment conditions are displayed in the review step");
                        }

                        QUnit.start();
                    }, EditPolicyView.$el.find('#reviewInfo'), "templates/policy/policies/ReviewPolicyStepTemplate.html");
                });
            });

            QUnit.asyncTest("Create new policy", function () {
                EditPolicyView.element = $("<div>")[0];
                $("#qunit-fixture").append(EditPolicyView.element);

                EditPolicyView.render(['iPlanetAMWebAgentService'], function () {
                    var activeTab = EditPolicyView.$el.find('#accordion .panel-collapse.in');

                    if (!activeTab.length) {
                        activeTab = EditPolicyView.$el.find('#accordion .panel-collapse.collapsing');
                    }
                    QUnit.ok(activeTab.parent().index() === 0, "First step of accordion is selected");
                    QUnit.ok(EditPolicyView.$el.find('h1 a.btn-default').length, "Cancel button is available");

                    QUnit.ok(EditPolicyView.$el.find('#policyName').val() === '', "Name is empty");
                    QUnit.ok(EditPolicyView.$el.find('#description').val() === '', "Description is empty");

                    PolicyResourcesView.render({}, function () {
                        CreatedResourcesView.render({}, function () {
                            var resources = CreatedResourcesView.$el.find('.res-name');
                            QUnit.ok(resources.length === 0, "No resources present");

                            PolicyActionsView.render(EditPolicyView.data, function () {
                                QUnit.equal(PolicyActionsView.$el.find('.alert.alert-info').length, 1, 'No actions are available for selection as resource type is not selected');

                                ReviewInfoView.render({}, function () {
                                    QUnit.ok(EditPolicyView.$el.find('#reviewName').hasClass('text-danger'), 'Name field is marked as invalid');
                                    QUnit.equal(EditPolicyView.$el.find('#reviewRes').length, 0, 'Resources field is not displayed in review step as it is invalid');
                                    QUnit.ok(EditPolicyView.$el.find('[name="submitForm"]').is(':disabled'), 'Submit button is disabled');

                                    QUnit.start();
                                });
                            });
                        });
                    });
                });
            });

            QUnit.asyncTest("List policies", function () {
                var appName = 'iPlanetAMWebAgentService';

                PolicyListView.element = $("<div>")[0];
                $("#qunit-fixture").append(PolicyListView.element);

                PolicyListView.render([appName], function () {
                    var table = $('#policies', PolicyListView.$el),
                        rowData = table.jqGrid('getRowData'),
                        postedData = table.jqGrid('getGridParam', 'postData'),
                        recordsTotal = table.jqGrid('getGridParam', 'records'),
                        totalNumberOfPages = table.jqGrid('getGridParam', 'lastpage'),
                        recordsPerPage = table.jqGrid('getGridParam', 'rowNum'),
                        rowList = table.jqGrid('getGridParam', 'rowList'),
                        remaining = table.jqGrid('getGridParam', 'userData').remaining;

                    QUnit.equal(PolicyListView.$el.find('.ui-jqgrid').length, 1, 'Table is rendered');
                    QUnit.equal(PolicyListView.$el.find('.global-actions').length, 1, 'Table actions are rendered');

                    QUnit.equal(table.find("tr[id]").length, rowData.length, "Number of rows in grid match number displayed");

                    // assuming application property "editable" is true
                    QUnit.equal(PolicyListView.$el.find('#appNameHeader').find('a').text(), appName, 'App name is displayed correctly');
                    QUnit.equal(PolicyListView.$el.find('#appNameHeader').find('a').attr('href'), '#app/' + appName, 'App link is correct');

                    QUnit.equal(PolicyListView.$el.find('#backToApps').length, 1, "Back button is available");
                    QUnit.equal(PolicyListView.$el.find('.navtable').length, 1, 'Columns Button is available');

                    QUnit.ok(PolicyListView.$el.find('#policies_name').find('span[sort=desc]').hasClass('ui-state-disabled'), 'Name is sorted in ascending order');

                    QUnit.equal(PolicyListView.$el.find('#policiesPager').length, 1, 'Pager is present');

                    QUnit.equal(PolicyListView.$el.find('#policiesPager_left').find('.ui-icon-add').length, 1, 'Columns icon is present');
                    QUnit.equal(PolicyListView.$el.find('#policiesPager_left').find('.ui-icon-search').length, 1, 'Advanced search icon is present');

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

            module('Resource Types');

            QUnit.asyncTest("Edit Resource Type", function () {
                var resTypeUUID = '6a90eabe-9638-4333-b688-3223aec7f58a';

                EditResourceTypeView.element = $("<div>")[0];
                $("#qunit-fixture").append(EditResourceTypeView.element);

                EditResourceTypeView.render([resTypeUUID], function () {
                    var entity = EditResourceTypeView.data.entity,
                        actions = EditResourceTypeView.data.actions,
                        actionsList = new ResTypeActionsView(),
                        patternList = new ResTypePatternsView(),
                        activeTab = EditResourceTypeView.$el.find('#accordion .panel-collapse.in');

                    if (!activeTab.length) {
                        activeTab = EditResourceTypeView.$el.find('#accordion .panel-collapse.collapsing');
                    }

                    // Step 1
                    QUnit.ok(EditResourceTypeView.validationFields && EditResourceTypeView.validationFields.length > 0, 'Validation is present');
                    QUnit.equal(EditResourceTypeView.$el.find('[name="submitForm"]').is(':disabled'), false, 'Submit button is enabled');

                    QUnit.equal(activeTab.parent().index(), 2, "Last step of accordion is selected");
                    QUnit.equal(EditResourceTypeView.$el.find('h1 a.btn-default').length, 1, "Cancel button is present");

                    QUnit.equal(EditResourceTypeView.$el.find('#resTypeName').val(), entity.name, "Name is set");

                    QUnit.ok(EditResourceTypeView.$el.find('#resTypeDescription').val() === (entity.description ? entity.description : ''), "Description is set");

                    // Step 2
                    actionsList.render(entity, actions, '#resTypeActions', function () {
                        var listItems = EditResourceTypeView.$el.find('#resTypeActions ul li'),
                            valid = true;

                        _.each(listItems, function (item) {
                            valid = valid && $(item).text().trim() === item.dataset.itemName;
                        });

                        // last item is 'add new' item
                        QUnit.equal(listItems.length - 1, EditResourceTypeView.data.actions.length, "Correct number of actions are displayed");
                        QUnit.ok(valid, "Action names are displayed correctly");

                        QUnit.ok(listItems.last().hasClass('editing'), "Add new action element is displayed");

                        patternList.render(entity, entity.patterns, '#resTypePatterns', function () {
                            var listItems = EditResourceTypeView.$el.find('#resTypePatterns ul li'),
                                valid = true;

                            // last item is 'add new' item
                            QUnit.equal(listItems.length - 1, entity.patterns.length, "Correct number of patterns are displayed");
                            _.each(listItems, function (item) {
                                valid = valid && $(item).text().trim() === item.dataset.itemName;
                            });
                            QUnit.ok(valid, "Patterns are displayed correctly");

                            QUnit.ok(listItems.last().hasClass('editing'), "Add new patern element is displayed");

                            // Step 3
                            $('#reviewInfo', EditResourceTypeView.$el).html(UIUtils.fillTemplateWithData('templates/policy/resourcetypes/ReviewResourceTypeStepTemplate.html', EditResourceTypeView.data, function () {
                                QUnit.ok(EditResourceTypeView.$el.find('#reviewName').text().trim() === entity.name, "Correct name is displayed in the review step");
                                if ((!EditResourceTypeView.$el.find('#reviewDesc').html()) && (!entity.description)) {
                                    // both are undefined.
                                    QUnit.ok(true, "Correct description is displayed in the review step");
                                } else {
                                    QUnit.ok(EditResourceTypeView.$el.find('#reviewDesc').html().trim() === (entity.description ? entity.description : ''), "Correct description is displayed in the review step");
                                }

                                // Patterns
                                if (entity.patterns.length) {
                                    var patterns = [];
                                    _.each(EditResourceTypeView.$el.find('ul#reviewPatterns').find('li'), function (value, key) {
                                        patterns[key] = value.innerHTML;
                                    });

                                    QUnit.ok(_.isEqual(patterns, entity.patterns), "Correct patterns are displayed in the review step");
                                }

                                // Actions
                                if (entity.actions.length) {
                                    QUnit.ok(_.isEqual(EditResourceTypeView.$el.find('ul#reviewActions').find('li').lenght, entity.actions.length), "Correct count actions are displayed in the review step");

                                    var valid = true;

                                    _.each(EditResourceTypeView.$el.find('ul#reviewActions').find('li'), function (item) {
                                        valid = valid && $(item).text().trim() === item.dataset.itemName;
                                    });

                                    QUnit.ok(valid, "Correct actions are displayed in the review step");
                                }

                                QUnit.start();
                            }));
                        });
                    });
                });
            });

            QUnit.asyncTest("Create new resource type", function () {
                EditResourceTypeView.element = $("<div>")[0];
                $("#qunit-fixture").append(EditResourceTypeView.element);

                EditResourceTypeView.render([], function () {
                    var entity = EditResourceTypeView.data.entity,
                        actions = EditResourceTypeView.data.actions,
                        actionsList = new ResTypeActionsView(),
                        patternList = new ResTypePatternsView(),
                        activeTab = EditResourceTypeView.$el.find('#accordion .panel-collapse.in');

                    if (!activeTab.length) {
                        activeTab = EditResourceTypeView.$el.find('#accordion .panel-collapse.collapsing');
                    }

                    QUnit.ok(activeTab.parent().index() === 0, "First step of accordion is selected");
                    QUnit.ok(EditResourceTypeView.$el.find('h1 a.btn-default').length, "Cancel button is available");

                    // Step 1
                    QUnit.ok(EditResourceTypeView.$el.find('#resTypeName').val() === '', "Name is empty");
                    QUnit.ok(EditResourceTypeView.$el.find('#resTypeDescription').val() === '', "Description is empty");

                    // Step 2
                    actionsList.render(entity, EditResourceTypeView.data.actions, '#resTypeActions', function () {
                        var listItems = EditResourceTypeView.$el.find('#resTypeActions ul li');

                        // first element is 'no item', second is 'add new' item
                        QUnit.equal(listItems.length, 2, "Only default items are displayed");

                        patternList.render(entity, entity.patterns, '#resTypePatterns', function () {
                            var listItems = EditResourceTypeView.$el.find('#resTypePatterns ul li');

                            // first element is 'no item', second is 'add new' item
                            QUnit.equal(listItems.length, 2, "Only default items are displayed");

                            // Step 3
                            ReviewInfoView.render(EditResourceTypeView.data, function () {
                                QUnit.ok(EditResourceTypeView.$el.find('#reviewName').hasClass('text-danger'), 'Name field is marked as invalid');
                                QUnit.equal(EditResourceTypeView.$el.find('#reviewRes').length,0, 'Resources field is not displayed in review step as it is invalid');
                                QUnit.ok(EditResourceTypeView.$el.find('[name="submitForm"]').is(':disabled'), 'Submit button is disabled');

                                QUnit.start();
                            }, EditResourceTypeView.$el.find('#reviewInfo'), "templates/policy/resourcetypes/ReviewResourceTypeStepTemplate.html");
                        });
                    });
                });
            });

            QUnit.asyncTest("List all resource types", function () {
                ResTypesListView.element = $("<div>")[0];
                $("#qunit-fixture").append(ResTypesListView.element);

                ResTypesListView.render([], function () {
                    var table = $('#resTypes', ResTypesListView.$el),
                        postedData = table.jqGrid('getGridParam', 'postData'),
                        rowData = table.jqGrid('getRowData'),
                        recordsTotal = table.jqGrid('getGridParam', 'records'),
                        totalNumberOfPages = table.jqGrid('getGridParam', 'lastpage'),
                        recordsPerPage = table.jqGrid('getGridParam', 'rowNum'),
                        rowList = table.jqGrid('getGridParam', 'rowList'),
                        remaining = table.jqGrid('getGridParam', 'userData').remaining;


                    QUnit.equal(ResTypesListView.$el.find('.ui-jqgrid').length, 1, 'Table is rendered');
                    QUnit.equal(ResTypesListView.$el.find('.global-actions').length, 1, 'Table actions are rendered');

                    QUnit.ok(rowData.length > 0, "At least one resource type listed in the table");
                    QUnit.ok(rowData.length === table.find("tr[id]").length, "Number of rows in grid match number displayed");

                    QUnit.ok(table.jqGrid('getGridParam', 'colNames').length === table.find("tr[id]")[0].children.length,
                        'Total number of columns displayed matches number of columns requested');

                    // sorting
                    QUnit.ok(ResTypesListView.$el.find('#resTypes_name').find('.s-ico').length === 1,
                        'Sort icon is present for the name column');

                    QUnit.ok(ResTypesListView.$el.find('#resTypes_name').find('span[sort=desc]').hasClass('ui-state-disabled'),
                        'Name is sorted in ascending order');

                    // Pagination
                    QUnit.ok($('#resTypesPager', ResTypesListView.$el).length === 1, 'Pager is present');

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
                    QUnit.ok($('.navtable', ResTypesListView.$el).length === 1, 'Columns Button is available');
                    QUnit.start();
                });
            });

            module('Common');

            QUnit.asyncTest("Unauthorized GET Request", function () {
                var viewManager = require('org/forgerock/commons/ui/common/main/ViewManager');
                Configuration.loggedUser = {"roles": ["ui-admin"]};
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.manageApps,
                    callback: function () {
                        sinon.stub(viewManager, 'showDialog', function () {
                            QUnit.ok(true, "Login dialog is shown");
                            QUnit.start();
                            delete Configuration.globalData.authorizationFailurePending;
                            viewManager.showDialog.restore();
                        });

                        EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, {error: {type: "GET"} });
                    }
                });
            });

            QUnit.asyncTest("Unauthorized POST Request", function () {
                var viewManager = require('org/forgerock/commons/ui/common/main/ViewManager');

                sinon.stub(viewManager, 'showDialog', function () {
                    QUnit.ok(true, "Login dialog is shown");
                    QUnit.start();
                    delete Configuration.globalData.authorizationFailurePending;
                    viewManager.showDialog.restore();
                });

                QUnit.ok(!viewManager.showDialog.called, "Login Dialog render function has not yet been called");
                Configuration.loggedUser = {"roles": ["ui-admin"]};
                EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, {error: {type: "POST"} });
                QUnit.ok(Configuration.loggedUser !== null, "User info should be retained after UNAUTHORIZED POST error");
            });

            QUnit.test("Add/Edit routes with different input", function () {
                QUnit.equal(Router.getLink(Router.configuration.routes.editApp, [null]), "app/", "Add App - no arguments provided");
                QUnit.equal(Router.getLink(Router.configuration.routes.editApp, ["calendar"]), "app/calendar", "Edit App with one argument provided");
                QUnit.equal(Router.getLink(Router.configuration.routes.editApp, ["test spaces"]), "app/test spaces", "Edit App with space in the name");
                QUnit.equal(Router.getLink(Router.configuration.routes.editPolicy, ["calendar", null]), "app/calendar/policy/", "Add policy with one argument provided");
                QUnit.equal(Router.getLink(Router.configuration.routes.editPolicy, ["calendar", "testPolicy"]), "app/calendar/policy/testPolicy", "Edit policy with two arguments provided");
            });
        }
    }
});