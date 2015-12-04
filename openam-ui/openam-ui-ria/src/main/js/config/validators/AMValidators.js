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

/*global define */

define("config/validators/AMValidators", [
    "jquery"
], function ($) {
    var obj = {
        "validPhoneFormat": {
            "name": "Valid Phone Number",
            "dependencies": [],
            "validator": function (el, input, callback) {
                var phonePattern = /^\+?([0-9\- \(\)])*$/,
                    value = input.val();

                if (typeof value === "string" && value.length && !phonePattern.test(value)) {
                    callback([$.t("common.form.validation.VALID_PHONE_FORMAT")]);
                } else {
                    callback();
                }
            }
        },
        "validEmailAddressFormat": {
            "name": "Valid Email Address",
            "dependencies": [],
            "validator": function (el, input, callback) {
                var emailPattern = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/,
                    value = input.val();

                if (typeof value === "string" && value.length && !emailPattern.test(value)) {
                    callback([$.t("common.form.validation.VALID_EMAIL_ADDRESS_FORMAT")]);
                } else {
                    callback();
                }
            }
        }
    };
    return obj;
});
