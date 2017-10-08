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

define([
    "lodash",
    "org/forgerock/commons/ui/common/main/Router"
], (_, Router) => {

    function throwOnNoData (data) {
        if (!data) {
            throw new Error("[createTreeNavigation] No \"data\" array found.");
        } else if (data && !_.isArray(data)) {
            throw new Error("[createTreeNavigation] \"data\" is not an array.");
        }
    }

    function throwOnArgsNotArray (args) {
        if (args && !_.isArray(args)) {
            throw new Error("[createTreeNavigation] \"args\" is not an array.");
        }
    }

    /**
     * @param  {object[]} data An array of navigation objects
     * @param  {string} data[].title The navigation link title. This can be a translation string.
     * @param  {string} data[].icon The navigation link icon
     * @param  {string} [data[].route] Each data[] object needs to have one of the following,
     *                                 [data[].route], or [data[].event], or [data[].children].
     *                                 If a [data[].route] is supplied, the Router will convert this value into an href
     * @param  {string} [data[].event] If a [data[].event] is supplied, the event will be added via the template
     * @param  {array} [data[].children] An array of child navigation objects of the same format as this.
     * @param  {array} [args] An array of routing arguments.
     * @example
     * <code>
     * data = [{
     *     title: "console.common.navigation.foo",
     *     icon: "fa-check-triangle-o",
     *     route: "myRouteKey"
     * }, {
     *     title: "console.common.navigation.privileges",
     *     icon: "fa-check-square-o",
     *     event: "main.navigation.EVENT_REDIRECT_TO_JATO_PRIVILEGES"
     * }, {
     *     title: "console.common.navigation.authorization",
     *     icon: "fa-key",
     *     children: [{
     *         title: "console.common.navigation.policySets",
     *         icon: "fa-angle-right",
     *         route: "realmsPolicySets"
     *     }, {
     *         title: "console.common.navigation.resourceTypes",
     *         icon: "fa-angle-right",
     *         route: "realmsResourceTypes"
     *     }]
     * }]
     * </code>
     * @returns {object[]} Returns the same array with an object[].href added to each navigation object.
     */
    function createTreeNavigation (data, args) {

        throwOnNoData(data);
        throwOnArgsNotArray(args);

        _.each(data, (navObj) => {
            if (navObj.route) {
                navObj.href = `#${Router.getLink(Router.configuration.routes[navObj.route], args)}`;
            } else if (navObj.event) {
                navObj.href = "#";
            } else if (navObj.children) {
                navObj.href = _.camelCase(navObj.title);
                createTreeNavigation(navObj.children, args);
            }
        });
        return data;
    }
    return createTreeNavigation;
});
