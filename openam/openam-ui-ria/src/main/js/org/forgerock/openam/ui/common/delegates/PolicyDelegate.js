/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

/*global $, define, _ */
/*jslint sub:true */

define("org/forgerock/openam/ui/common/delegates/PolicyDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function (constants, AbstractDelegate) {
    var obj = new AbstractDelegate("/policy");

    obj.readEntity = function (baseEntity, callback) {
        // this is a mock server call, responding with what would be expected of a server-side policy validation service.
        // (for example, as implemented in OpenIDM)
        var promise = $.Deferred(),
            functions = {
                "required": "\nfunction (fullObject, value, params, propName) {\n    if (value === undefined) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    }\n    return [];\n}\n",
                "not-empty": "\nfunction (fullObject, value, params, property) {\n    if (value !== undefined && (value === null || !value.length)) {\n        return [{\"policyRequirement\":\"REQUIRED\"}];\n    } else {\n        return [];\n    }\n}\n",
                "valid-phone-format": "\nfunction (fullObject, value, params, property) {\n    var phonePattern = /^\\+?([0-9\\- \\(\\)])*$/;\n    \n        if (typeof(value) === \"string\" && value.length && !phonePattern.test(value)) {\n            return [ {\"policyRequirement\": \"VALID_PHONE_FORMAT\"}];\n        } else {\n            return [];\n        }\n    }",
                "valid-email-address-format": "\nfunction (fullObject, value, params, property) {\n    var emailPattern = /^([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})$/; \n        \n        if (typeof(value) === \"string\" && value.length && !emailPattern.test(value)) {\n            return [ {\"policyRequirement\": \"VALID_EMAIL_ADDRESS_FORMAT\"}];\n        } else {\n            return [];\n        }\n    }"
            },
            response = {};

        if (baseEntity.match(new RegExp("users\/[^\/]+$"))) {
            response = {
                        "resource": baseEntity,
                        "properties": [
                            {
                                "policyRequirements": [
                                    "REQUIRED"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "required",
                                        "policyFunction": functions["required"]
                                    },
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "not-empty",
                                        "policyFunction": functions["not-empty"]
                                    }
                                ],
                                "name": "uid"
                            },
                            {
                                "policyRequirements": [
                                    "VALID_EMAIL_ADDRESS_FORMAT"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "VALID_EMAIL_ADDRESS_FORMAT"
                                        ],
                                        "policyId": "valid-email-address-format",
                                        "policyFunction": functions["valid-email-address-format"]
                                    }
                                ],
                                "name": "mail"
                            },
                            {
                                "policyRequirements": [
                                    "REQUIRED"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "required",
                                        "policyFunction": functions["required"]
                                    },
                                    {
                                        "policyRequirements": [
                                            "REQUIRED"
                                        ],
                                        "policyId": "not-empty",
                                        "policyFunction": functions["not-empty"]
                                    }
                                ],
                                "name": "sn"
                            },
                            {
                                "policyRequirements": [
                                    "VALID_PHONE_FORMAT"
                                ],
                                "policies": [
                                    {
                                        "policyRequirements": [
                                            "VALID_PHONE_FORMAT"
                                        ],
                                        "policyId": "valid-phone-format",
                                        "policyFunction": functions["valid-phone-format"]
                                    }
                                ],
                                "name": "telephoneNumber"
                            }
                        ]
                    };
        }

        promise.resolve(response);

        if (callback) {
            callback(response);
        }

        return promise;

    };

    return obj;
});



