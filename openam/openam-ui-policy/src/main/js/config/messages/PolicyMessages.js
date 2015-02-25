/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/**
 * @author Eugenia Sergueeva
 */

/*global define*/

define("config/messages/PolicyMessages", [], function () {
    return {
        "applicationCreated": {
            msg: "config.messages.PolicyMessages.applicationCreated",
            type: "info"
        },
        "applicationUpdated": {
            msg: "config.messages.PolicyMessages.applicationUpdated",
            type: "info"
        },
        "referralCreated": {
            msg: "config.messages.PolicyMessages.referralCreated",
            type: "info"
        },
        "referralUpdated": {
            msg: "config.messages.PolicyMessages.referralUpdated",
            type: "info"
        },
        "deleteSuccess": {
            msg: "config.messages.PolicyMessages.deleteSuccess",
            type: "info"
        },
        "deleteFail": {
            msg: "config.messages.PolicyMessages.deleteFail",
            type: "error"
        },
        "policyCreated": {
            msg: "config.messages.PolicyMessages.policyCreated",
            type: "info"
        },
        "policyUpdated": {
            msg: "config.messages.PolicyMessages.policyUpdated",
            type: "info"
        },
        "ruleErrorFullLogical": {
            msg: "config.messages.PolicyMessages.ruleErrorFullLogical",
            type: "error"
        },
        "ruleHelperTryAndOr": {
            msg: "config.messages.PolicyMessages.ruleHelperTryAndOr",
            type: "info"
        },
        "unableToRetrievePolicy": {
            msg: "config.messages.PolicyMessages.unableToRetrievePolicy",
            type: "error"
        },
        "invalidResource": {
            msg: "config.messages.PolicyMessages.invalidResource",
            type: "error"
        },
        "duplicateAttribute": {
            msg: "config.messages.PolicyMessages.duplicateAttribute",
            type: "error"
        },
        "duplicateResource": {
            msg: "config.messages.PolicyMessages.duplicateResource",
            type: "error"
        },
        "duplicateRealm": {
            msg: "config.messages.PolicyMessages.duplicateRealm",
            type: "error"
        },
        "unableToPersistPolicy": {
            msg: "config.messages.PolicyMessages.unableToPersistPolicy",
            type: "error"
        },
        "policiesUploaded": {
            msg: "config.messages.PolicyMessages.policiesUploaded",
            type: "info"
        },
        "policiesUploadFailed": {
            msg: "config.messages.PolicyMessages.policiesUploadFailed",
            type: "error"
        }
    };
});
