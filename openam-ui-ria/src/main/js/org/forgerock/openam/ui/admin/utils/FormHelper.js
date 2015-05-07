/*
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

/*global $ _ define*/
define("org/forgerock/openam/ui/admin/utils/FormHelper", [
    "jsonEditor"
], function(JSONEditor) {
    var obj = {};

    obj.bindSavePromiseToElement = function(promise, element) {
        element = $(element);

        var originalText = element.text();

        element.prop('disabled', true)
        .text('')
        .prepend($('<span class="fa fa-refresh fa-spin"/>'));

        promise.always(function() {
            element.prop('disabled', false)
            .text(' ' + originalText);
        })
        .done(function() {
            element.prepend($('<span class="fa fa-check fa-fw"/>'));
        })
        .fail(function() {
            element.prepend($('<span class="fa fa-times fa-fw"/>'));
        })
        .always(function() {
            _.delay(function() {
                element.empty()
                .text(originalText);
            }, 2000);
        });
    };

    return obj;
});
