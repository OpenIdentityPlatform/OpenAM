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

/*global define*/

define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/CriteriaView", [
    "org/forgerock/commons/ui/common/main/AbstractView"
], function(AbstractView) {

    var CriteriaView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/CriteriaTemplate.html",
        REQUIRED: "required",

        /**
         * sets the criteria and re-renders the template.
         */
        setCriteria: function(criteria){
            this.data.linkConfig.criteria = criteria;
            this.parentRender();
        },

        /**
         * sets the passThroughFail data object and re-renders the template. 
         */
        setPassThroughAndFailArrows: function(boolean){
            this.data.passThroughFail = boolean;
            this.parentRender();
        },

        render: function (linkConfig, allCriteria, id) {
            this.data = {};
            this.data.allCriteria = allCriteria;
            this.data.linkConfig = linkConfig;
            this.data.id = id;
            this.data.passThroughFail = linkConfig.criteria.toLowerCase() === this.REQUIRED ? true : false;
            this.parentRender();
        }
    });

    return CriteriaView;

});
