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
 * Copyright 2016 ForgeRock AS.
 */

define([], () => {

    /**
     * Warns if a property is inferred to be a password and does not have a format of password
     * @param {Object} property Property to transform
     * @param {String} name Raw property name
     */
    return function warnOnInferredPasswordWithoutFormat (property, name) {
        const possiblePassword = name.toLowerCase().indexOf("password", name.length - 8) !== -1;
        const hasFormat = property.format === "password";
        if (property.type === "string" && possiblePassword && !hasFormat) {
            console.error(`[cleanJSONSchema] Detected (inferred) a password property \"${name}\" ` +
                "without format attribute of \"password\"");
        }
    };
});
