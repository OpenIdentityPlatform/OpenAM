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

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/AddNewResourceView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/ResourcesListView"
], function (AbstractView, resourcesListView) {
    var AddNewResourceView = AbstractView.extend({
        element: "#patterns",
        template: "templates/policy/AddNewResourceTemplate.html",
        noBaseTemplate: true,
        events: {
            'click .addPattern': 'addPattern',
            'keyup .addPattern': 'addPattern'
        },

        render: function (args, callback) {

            _.extend(this.data, args);

            this.parentRender(function () {
                this.$newResource = this.$el.find('.resource-url-part');

                if (callback) {
                    callback();
                }
            });

        },

        addPattern: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            this.data.options.newPattern = $(e.currentTarget).find(".pattern").text();
            resourcesListView.render(this.data);
        }

    });

    return new AddNewResourceView();
});