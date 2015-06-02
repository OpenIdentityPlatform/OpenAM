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

/*global require, define, QUnit, $ */

define([
    "org/forgerock/openam/ui/policy/applications/EditApplicationView",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/policies/EditPolicyView",
    "org/forgerock/openam/ui/policy/resources/CreatedResourcesView",
    "org/forgerock/openam/ui/policy/resourcetypes/EditResourceTypeView"
], function (EditAppView, PolicyDelegate, EditPolicyView, CreatedResourcesView, EditResourceTypeView) {

    return {
        executeAll: function () {
            QUnit.module('Edit Views');

            QUnit.asyncTest('Create New Application', function () {
                EditAppView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditAppView.element);

                EditAppView.render([], function () {
                    var activeTab = EditAppView.$el.find('#accordion .panel-collapse.in');
                    if (!activeTab.length) {
                        activeTab = EditAppView.$el.find('#accordion .panel-collapse.collapsing');
                    }

                    QUnit.ok(activeTab.parent().index() === 0, "First step of accordion is selected");
                    QUnit.ok(EditAppView.$el.find('#reviewName').hasClass('text-danger'), "Name does not pass validation");
                    QUnit.ok(EditAppView.$el.find('input[name="submitForm"]').is(':disabled'), "Finish button is disabled");
                    QUnit.start();
                });
            });

            QUnit.asyncTest('Edit Application', function () {
                EditAppView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditAppView.element);

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
                    var selected = _.findByValues(EditAppView.data.options.allResourceTypes, 'uuid', EditAppView.data.entity.resourceTypeUuids),
                        leftItems = EditAppView.resourceTypesListView.$el.find('.list-table ul li:not(.text-danger)'),
                        rightItems = EditAppView.resourceTypesListSelectedView.$el.find('.list-table ul li:not(.text-danger)'),
                        initialRight = rightItems.length;

                    // select and deselect resource types
                    // add 2
                    $(leftItems[0]).trigger('click');
                    $(leftItems[1]).trigger('click');

                    // remove 1
                    $(rightItems[0]).find('.icon-close').trigger('click');

                    QUnit.equal(entity.resourceTypeUuids.length, initialRight + 1, 'Resource Types can be successfully selected and deselected');

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
                });
            });

            QUnit.asyncTest('Create New Policy', function () {
                EditPolicyView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditPolicyView.element);

                EditPolicyView.render(['iPlanetAMWebAgentService'], function () {
                    var activeTab = EditPolicyView.$el.find('#accordion .panel-collapse.in');

                    if (!activeTab.length) {
                        activeTab = EditPolicyView.$el.find('#accordion .panel-collapse.collapsing');
                    }
                    QUnit.ok(activeTab.parent().index() === 0, "First step of accordion is selected");

                    QUnit.ok(EditPolicyView.$el.find('#reviewName').hasClass('text-danger'), 'Name field is marked as invalid');
                    QUnit.equal(EditPolicyView.$el.find('#reviewRes').length, 0, 'Resources field is not displayed in review step as it is invalid');
                    QUnit.ok(EditPolicyView.$el.find('[name="submitForm"]').is(':disabled'), 'Submit button is disabled');

                    QUnit.start();
                });
            });

            QUnit.asyncTest('Edit Policy: Resources', function () {
                var appName = 'iPlanetAMWebAgentService',
                    policyName = 'test';

                EditPolicyView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditPolicyView.element);

                EditPolicyView.render([appName, policyName], function () {
                    var entity = EditPolicyView.data.entity,
                        options = EditPolicyView.data.options,
                        createdResources = EditPolicyView.$el.find('#createdResources li'),
                        correctResources = true;

                    _.each(createdResources, function (item) {
                        correctResources = correctResources && $(item).text().trim() === item.dataset.resource && _.contains(entity.resources, item.dataset.resource);
                    });

                    QUnit.equal(createdResources.length, entity.resources.length, "Correct number of resources are displayed");
                    QUnit.ok(correctResources, "Resources are displayed correctly");

                    // Available Patterns
                    var patterns = EditPolicyView.$el.find('#patterns').find('.list-table ul li'),
                        pattern = EditPolicyView.$el.find('#patterns').find('.list-group-item:eq(0)'),
                        availablePatterns = [];

                    _.each(patterns, function (item) {
                        availablePatterns.push($(item).find('span:first-of-type').text());
                    });

                    QUnit.equal(_.difference(availablePatterns, options.availablePatterns).length, 0, "All available patterns are displayed correctly");

                    pattern.trigger('click');
                    QUnit.equal(pattern.find('span:first-of-type').text(), options.newPattern, "New pattern selected");

                    CreatedResourcesView.render(EditPolicyView.data, function () {
                        var editing = EditPolicyView.$el.find('#createdResources .editing'),
                            listItems = EditPolicyView.$el.find('#createdResources ul li:not(.editing)'),
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
                        listItems = EditPolicyView.$el.find('#createdResources ul li:not(.editing)');
                        var lastAddedItem = listItems.eq(listItems.length - 1);

                        lastAddedItem.find('.fa-close').trigger('click');
                        QUnit.ok(entity.resources.length === resourceLength - 1 && !_.contains(entity.resources, lastAddedItem.data().resource), 'Resource deleted');

                        QUnit.start();
                    });
                });
            });

            QUnit.asyncTest('Edit Policy: User Attributes', function () {
                var appName = 'iPlanetAMWebAgentService',
                    policyName = 'test';

                EditPolicyView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditPolicyView.element);

                EditPolicyView.render([appName, policyName], function () {
                    $.when(PolicyDelegate.getAllUserAttributes()).done(function () {
                        var staticAttributes = _.where(EditPolicyView.data.entity.resourceAttributes, {type: "Static"}),
                            userAttributes = _.where(EditPolicyView.data.entity.resourceAttributes, {type: "User"});

                        staticAttributes.push({ "type": "Static", "propertyName": "test", "propertyValues": ["test"] });

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

                        QUnit.equal(EditPolicyView.$el.find('#userAttrs').find('.selectize-input').find('.item').length, userAttributes.length, 'User attributes are selected correctly');
                        QUnit.start();
                    });
                });
            });

            QUnit.asyncTest('Edit Policy: Actions', function () {
                var appName = 'iPlanetAMWebAgentService',
                    policyName = 'test';

                EditPolicyView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditPolicyView.element);

                EditPolicyView.render([appName, policyName], function () {
                    var entity = EditPolicyView.data.entity,
                        resourceTypesActions = EditPolicyView.data.options.availableActions,
                        resourceTypesActionsInitialLength = resourceTypesActions.length,
                        availableActions = EditPolicyView.$el.find('#availableActions').find('li'),
                        correctPermissions = true,
                        action;

                    _.each(resourceTypesActions, function (rtAction) {
                        action = _.findWhere(availableActions, function (action) {
                            return rtAction.action === $(action).data('listItem');
                        });

                        correctPermissions = correctPermissions && ($(action).length === 1 &&
                            rtAction.value === ($(action).find('input:checked').val() === 'true'));
                    });
                    QUnit.ok(correctPermissions, 'All actions and permissions are displayed correctly');

                    // Selecting individual actions
                    var row = EditPolicyView.$el.find('#availableActions').find('li:first-of-type'),
                        actionName = row.data('listItem');
                    row.trigger('click');
                    QUnit.equal(EditPolicyView.data.options.availableActions.length, resourceTypesActionsInitialLength - 1, 'Minus one available action');
                    QUnit.ok(actionName in EditPolicyView.data.entity.actionValues, 'Selected action is added to the entity');

                    QUnit.start();
                });
            });

            QUnit.asyncTest('Edit Policy: Review Step', function () {
                var appName = 'iPlanetAMWebAgentService',
                    policyName = 'test';

                EditPolicyView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditPolicyView.element);

                EditPolicyView.render([appName, policyName], function () {
                    var entity = EditPolicyView.data.entity;

                    QUnit.ok(EditPolicyView.$el.find('#reviewName').text().trim() === entity.name,
                        "Correct name is displayed in the review step");

                    if ((!EditPolicyView.$el.find('#reviewDesc').html()) && (!entity.description)) {
                        // both are undefined.
                        QUnit.ok(true, "Correct description is displayed in the review step");
                    } else {
                        QUnit.ok(EditPolicyView.$el.find('#reviewDesc').html().trim() === (entity.description ? entity.description : ''),
                            "Correct description is displayed in the review step");
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
                });
            });

            QUnit.asyncTest('Edit Policy: Common', function () {
                var appName = 'iPlanetAMWebAgentService',
                    policyName = 'test';

                EditPolicyView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditPolicyView.element);

                EditPolicyView.render([appName, policyName], function () {
                    var entity = EditPolicyView.data.entity,
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

                    QUnit.start();
                });
            });

            QUnit.asyncTest('Create New Resource Type', function () {
                EditResourceTypeView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditResourceTypeView.element);

                EditResourceTypeView.render([], function () {
                    var activeTab = EditResourceTypeView.$el.find('#accordion .panel-collapse.in');

                    if (!activeTab.length) {
                        activeTab = EditResourceTypeView.$el.find('#accordion .panel-collapse.collapsing');
                    }

                    QUnit.ok(activeTab.parent().index() === 0, "First step of accordion is selected");

                    QUnit.ok(EditResourceTypeView.$el.find('#reviewName').hasClass('text-danger'), 'Name field is marked as invalid');
                    QUnit.equal(EditResourceTypeView.$el.find('#reviewRes').length, 0, 'Resources field is not displayed in review step as it is invalid');
                    QUnit.ok(EditResourceTypeView.$el.find('[name="submitForm"]').is(':disabled'), 'Submit button is disabled');

                    QUnit.start();
                });
            });

            QUnit.asyncTest('Edit Resource Type', function () {
                var resTypeUUID = '6a90eabe-9638-4333-b688-3223aec7f58a';

                EditResourceTypeView.element = $('<div>')[0];
                $('#qunit-fixture').append(EditResourceTypeView.element);

                EditResourceTypeView.render([resTypeUUID], function () {
                    var entity = EditResourceTypeView.data.entity,
                        actions = EditResourceTypeView.data.actions,
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

                    QUnit.ok(EditResourceTypeView.$el.find('#resTypeDescription').val() === (entity.description ? entity.description : ''),
                        "Description is set");

                    // Step 2
                    var resTypeActions = EditResourceTypeView.$el.find('#resTypeActions ul li'),
                        resTypeActionsCorrect = true;

                    _.each(resTypeActions, function (item) {
                        resTypeActionsCorrect = resTypeActionsCorrect && $(item).text().trim() === item.dataset.itemName;
                    });

                    // last item is 'add new' item
                    QUnit.equal(resTypeActions.length - 1, EditResourceTypeView.data.actions.length, "Correct number of actions are displayed");
                    QUnit.ok(resTypeActionsCorrect, "Action names are displayed correctly");

                    QUnit.ok(resTypeActions.last().hasClass('editing'), "Add new action element is displayed");

                    var resTypePatterns = EditResourceTypeView.$el.find('#resTypePatterns ul li'),
                        resTypePatternsCorrect = true;

                    // last item is 'add new' item
                    QUnit.equal(resTypePatterns.length - 1, entity.patterns.length, "Correct number of patterns are displayed");
                    _.each(resTypePatterns, function (item) {
                        resTypePatternsCorrect = resTypePatternsCorrect && $(item).text().trim() === item.dataset.itemName;
                    });
                    QUnit.ok(resTypePatternsCorrect, "Patterns are displayed correctly");

                    QUnit.ok(resTypePatterns.last().hasClass('editing'), "Add new pattern element is displayed");

                    // Step 3
                    QUnit.ok(EditResourceTypeView.$el.find('#reviewName').text().trim() === entity.name, "Correct name is displayed in the review step");
                    if ((!EditResourceTypeView.$el.find('#reviewDesc').html()) && (!entity.description)) {
                        // both are undefined.
                        QUnit.ok(true, "Correct description is displayed in the review step");
                    } else {
                        QUnit.ok(EditResourceTypeView.$el.find('#reviewDesc').html().trim() === (entity.description ? entity.description : ''),
                            "Correct description is displayed in the review step");
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
                });
            });
        }
    }
});