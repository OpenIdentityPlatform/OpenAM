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

/*global define*/

define([
    "jquery",
    "handlebars",
    "lodash",
    "org/forgerock/openam/ui/common/util/ExternalLinks"
], function ($, Handlebars, _, ExternalLinks) {

    Handlebars.registerHelper("externalLink", function (key) {
        return _.get(ExternalLinks, key, "");
    });

    Handlebars.registerHelper("policyEditorResourceHelper", function () {
        var result = this.options.newPattern.replace("-*-", "̂");
        result = result.replace(/\*/g,
            '<input class="form-control" required type="text" value="*" placeholder="*" />');
        result = result.replace("̂",
            '<input class="form-control" required type="text" value="-*-" placeholder="-*-" pattern="[^/]+" />');

        return new Handlebars.SafeString(result);
    });

    // TODO: Commons Candidate
    Handlebars.registerHelper("debug", function () {
        console.warn("[handlebars] debug. Value of `this`");
        console.warn(this);
    });

    // TODO: should be removed once we upgrade to the newer version of handlebars
    /**
     * Handlebars parameterized translation helper
     * @example
     * 1) In translation file define a value under "key.to.my.translation.string" key,
     *    e.g. "Display __foo__ and __bar__"
     * 2) Call helper function with parameters: {{tWithParams "key.to.my.translation.string" foo="test1" bar="test2"}}
     * 3) Resulting string will be "Display test1 and test2"
     */
    Handlebars.registerHelper("tWithParams", function (translationKey, options) {
        var parameters = {},
            key;

        for (key in options.hash) {
            if (options.hash.hasOwnProperty(key)) {
                parameters[key] = options.hash[key];
            }
        }

        return $.t(translationKey, parameters);
    });
});
