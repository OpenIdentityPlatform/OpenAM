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
 * Copyright 2014 ForgeRock AS.
 */


/* For client-side.js */
var fontDetector = {detect: function() {}},
    console = {warn: function() {}},
    output = {value: ""},
    autoSubmitDelay = 0;
function submit() {}

/* For server-side.js */
var logger = {
        messageEnabled: function() {

        },
        warning: function() {

        },
        message: function() {

        }
    },
    username = "demo",
    clientScriptOutputData = "{}",
    idRepository = {
        getAttribute: function(username, attributeName) {
            return {
                iterator: function() {
                    return {
                        hasNext: function() {

                        }
                    }
                }
            }
        }
    }, FAILED, SUCCESS;

if (!String.prototype.trim) {
    String.prototype.trim = function () {
        return this.replace(/^\s+|\s+$/g, '');
    };
}
