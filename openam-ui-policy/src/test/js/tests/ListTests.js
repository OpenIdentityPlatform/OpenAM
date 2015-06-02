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
    "org/forgerock/openam/ui/policy/applications/ApplicationsListView",
    "org/forgerock/openam/ui/policy/policies/PoliciesListView",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypesListView",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (ApplicationsListView, PolicyListView, ResourceTypesListView, Configuration) {

    return {
        executeAll: function () {
            QUnit.module('List Views');

            QUnit.asyncTest('List Applications', function () {
                ApplicationsListView.element = $('<div>')[0];
                $('#qunit-fixture').append(ApplicationsListView.element);

                ApplicationsListView.render([], function () {
                    QUnit.ok(Configuration.globalData.policyEditor, 'Configuration file loaded');

                    QUnit.equal($('#gridToolbar', ApplicationsListView.$el).find('.btn:first-of-type').text().trim(),
                        $.t('policy.applications.list.add'), 'Add New button is present');
                    QUnit.equal($('#deleteRecords', ApplicationsListView.$el).length, 1, 'Delete Selected button is present');
                    QUnit.equal($('#importPolicies', ApplicationsListView.$el).length, 1, 'Import Policies button is present');
                    QUnit.equal($('#exportPolicies', ApplicationsListView.$el).length, 1, 'Export Policies button is present');

                    QUnit.ok($('#deleteRecords', ApplicationsListView.$el).hasClass('disabled'), 'Delete Selected button is disabled');

                    QUnit.ok($('#backgridContainer', ApplicationsListView.$el).html() !== '', 'Grid is present');
                    QUnit.ok($('#paginationContainer', ApplicationsListView.$el).html() !== '', 'Pagination is present');
                    QUnit.ok(ApplicationsListView.data.items.models.length > 0, 'Collection is not empty');

                    QUnit.start();
                });
            });

            QUnit.asyncTest('List Policies', function () {
                PolicyListView.element = $('<div>')[0];
                $('#qunit-fixture').append(PolicyListView.element);

                PolicyListView.render(['iPlanetAMWebAgentService'], function () {
                    QUnit.ok(Configuration.globalData.policyEditor, 'Configuration file loaded');

                    QUnit.equal($('.page-header', PolicyListView.$el).find('.btn').length, 1, "Back button is available");

                    QUnit.equal($('#gridToolbar', PolicyListView.$el).find('.btn:first-of-type').text().trim(),
                        $.t('policy.policies.list.add'), 'Add New button is present');
                    QUnit.equal($('#deleteRecords', PolicyListView.$el).length, 1, 'Delete Selected button is present');

                    QUnit.ok($('#deleteRecords', PolicyListView.$el).hasClass('disabled'), 'Delete Selected button is disabled');

                    QUnit.ok($('#backgridContainer', PolicyListView.$el).html() !== '', 'Grid is present');
                    QUnit.ok($('#paginationContainer', PolicyListView.$el).html() !== '', 'Pagination is present');
                    QUnit.ok(PolicyListView.data.items.models.length > 0, 'Collection is not empty');

                    QUnit.start();
                });
            });

            QUnit.asyncTest('List Resource Types', function () {
                ResourceTypesListView.element = $('<div>')[0];
                $('#qunit-fixture').append(ResourceTypesListView.element);

                ResourceTypesListView.render([], function () {
                    QUnit.ok(Configuration.globalData.policyEditor, 'Configuration file loaded');

                    QUnit.equal($('#gridToolbar', ResourceTypesListView.$el).find('.btn:first-of-type').text().trim(),
                        $.t('policy.resourceTypes.list.add'), 'Add New button is present');
                    QUnit.equal($('#deleteRecords', ResourceTypesListView.$el).length, 1, 'Delete Selected button is present');

                    QUnit.ok($('#deleteRecords', ResourceTypesListView.$el).hasClass('disabled'), 'Delete Selected button is disabled');

                    QUnit.ok($('#backgridContainer', ResourceTypesListView.$el).html() !== '', 'Grid is present');
                    QUnit.ok($('#paginationContainer', ResourceTypesListView.$el).html() !== '', 'Pagination is present');
                    QUnit.ok(ResourceTypesListView.data.items.models.length > 0, 'Collection is not empty');

                    QUnit.start();
                });
            });
        }
    }
});