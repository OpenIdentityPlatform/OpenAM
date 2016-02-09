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
 * Copyright 2016 ForgeRock AS.
 */

 /**
  * @module org/forgerock/openam/ui/admin/views/realms/services/renderForm
  */
define("org/forgerock/openam/ui/admin/views/realms/services/renderForm", [
    "lodash",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/models/Form"
], function (_, UIUtils, Form) {

    var exports = function ($el, data, callback) {
        UIUtils.fillTemplateWithData(
            "templates/admin/views/realms/services/FormTemplate.html",
            data,
            function (template) {
                var placeHolder = $el.find("[data-service-form-holder]");
                placeHolder.html(template);
                callback(new Form(placeHolder.find("[data-service-form]")[0], data.schema, data.values));
            }
        );
    };

    return exports;
});
