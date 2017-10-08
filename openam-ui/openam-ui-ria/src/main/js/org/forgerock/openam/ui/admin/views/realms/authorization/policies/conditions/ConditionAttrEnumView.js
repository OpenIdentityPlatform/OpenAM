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
 * Copyright 2015-2016 ForgeRock AS.
 */


define([
    "jquery",
    "lodash",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView"
], function ($, _, ConditionAttrBaseView) {
    return ConditionAttrBaseView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrEnum.html",

        render (data, element, callback) {
            this.initBasic(data, element, "field-float-select data-obj");

            this.parentRender(function () {
                if (callback) {
                    callback();
                }
            });
        }
    });
});
