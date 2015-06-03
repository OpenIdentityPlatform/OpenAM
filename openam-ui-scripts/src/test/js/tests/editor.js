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
    "org/forgerock/openam/ui/editor/views/ScriptListView",
    "org/forgerock/openam/ui/editor/views/EditScriptView"
], function (ScriptListView, EditScriptView) {
    return {
        executeAll: function () {

            asyncTest('Listing existing scripts', function () {
                ScriptListView.element = '<div></div>';
                $('#qunit-fixture').append(ScriptListView.$el);

                ScriptListView.render([], function () {
                    equal(ScriptListView.$el.find('.backgrid').length, 1, 'Backgrid renders');
                    start();
                });
            });

            asyncTest('Create Script', function () {
                EditScriptView.element = '<div></div>';
                $('#qunit-fixture').append(EditScriptView.$el);

                EditScriptView.render([], function () {
                    QUnit.ok(EditScriptView.$el.find('input[name="cancel"]').length, "Cancel button is available");
                    QUnit.ok(EditScriptView.$el.find('input[name="save"]').length, "Save button is available");

                    QUnit.ok(EditScriptView.$el.find('#name').val() === '', "Name is empty");
                    QUnit.ok(EditScriptView.$el.find('#script').val() === '', "Script code is empty");
                    QUnit.ok(EditScriptView.$el.find('#validation').empty(), 'Validation block is empty');

                    start();
                });
            });

            asyncTest('Edit Script', function () {
                EditScriptView.element = '<div></div>';
                $('#qunit-fixture').append(EditScriptView.$el);

                EditScriptView.render(['c20fa877-e9b5-486e-b555-1396ae0d7b76'], function () {
                    var entity = EditScriptView.data.entity;
                    ok(EditScriptView.$el.find('#name').val() === entity.name, 'Name is set');
                    ok(EditScriptView.$el.find('#script').val() === (entity.script ? entity.script : ''), 'Script code is set');
                    ok(EditScriptView.$el.find('input[name=language]:checked').val() === entity.language, 'Language is set');
                    ok(EditScriptView.$el.find('#context').html() === entity.context, 'Context is set');

                    start();
                });
            });
        }
    }
});