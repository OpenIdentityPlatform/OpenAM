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

/**
 * @module org/forgerock/openam/ui/admin/views/common/Backlink
 */
define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/commons/ui/common/util/UIUtils"
], ($, _, Backbone, URIUtils, UIUtils) => {

    function getBaseURI (allFragments, fragmentIndex) {
        return _.take(allFragments, fragmentIndex + 1).join("/");
    }

    return Backbone.View.extend({
        el:"#backlink",
        /**
         * Renders a back link navigation element.
         * @param {number} [fragmentIndex=1] Fragment index indicates which url fragment used for back link
         */
        render (fragmentIndex = 1) {
            const allFragments = URIUtils.getCurrentFragment().split("/");
            const pageFragment = allFragments[fragmentIndex];
            const data = {
                "title": $.t(`console.common.navigation.${pageFragment}`),
                "hash": `#${getBaseURI(allFragments, fragmentIndex)}`
            };

            UIUtils.fillTemplateWithData(
                "templates/admin/views/common/BackLink.html",
                data,
                (html) => {
                    this.$el.html(html);
                }
            );
        }
    });

});
