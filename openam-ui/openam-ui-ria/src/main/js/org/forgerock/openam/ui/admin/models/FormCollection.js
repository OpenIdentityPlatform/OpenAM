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

define("org/forgerock/openam/ui/admin/models/FormCollection", [
    "underscore"
], function (_) {
    var obj = function FormCollection () {
        this.forms = [];
    };

    obj.prototype.add = function (form) {
        this.forms.push(form);
    };

    obj.prototype.data = function () {
        return _.reduce(this.forms, function (merged, form) {
            return _.merge(merged, form.data());
        }, this.forms[0].data());
    };

    obj.prototype.reset = function () {
        _.each(this.forms, function (form) {
            form.reset();
        });
    };

    return obj;
});
