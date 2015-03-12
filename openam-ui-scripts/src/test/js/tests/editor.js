/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
    "org/forgerock/openam/ui/editor/views/ScriptListView",
    "org/forgerock/openam/ui/editor/views/EditScriptView"
], function (scriptListView, editScriptView) {
    return {
        executeAll: function () {

            asyncTest('Listing existing scripts', function () {
                scriptListView.element = '<div></div>';
                $('qunit-fixture').append(scriptListView.$el);

                scriptListView.render([], function () {
                    equal(scriptListView.$el.find('.backgrid').length, 1, 'Backgrid renders');
                    start();
                });
            });

            asyncTest('Create Script', function () {
                editScriptView.element = '<div></div>';
                $('qunit-fixture').append(editScriptView.$el);

                editScriptView.render([], function () {
                    var entity = editScriptView.data.entity;
                    QUnit.ok(editScriptView.$el.find('input[name="cancel"]').length, "Cancel button is available");
                    QUnit.ok(editScriptView.$el.find('input[name="save"]').length, "Save button is available");
                    QUnit.ok(editScriptView.$el.find('#scriptName').val() === '', "Name is empty");
                    QUnit.ok(editScriptView.$el.find('#scriptCode').val() === '', "Script code is empty");
                    start();
                });
            });

            asyncTest('Edit Script', function () {
                editScriptView.element = '<div></div>';
                $('qunit-fixture').append(editScriptView.$el);

                editScriptView.render(['53712aa4-0082-4e33-94a4-a6a2475a075f'], function () {
                    var entity = editScriptView.data.entity;
                    ok(editScriptView.$el.find('input[name="cancel"]').length, "Cancel button is available");
                    ok(editScriptView.$el.find('input[name="save"]').length, "Save button is available");
                    ok(editScriptView.$el.find('#scriptName').val() === entity.name, "Name is set");
                    ok(editScriptView.$el.find('#scriptCode').val() === (entity.script ? entity.script : ''), "Script code is set");
                    start();
                });
            });
        }
    }
});