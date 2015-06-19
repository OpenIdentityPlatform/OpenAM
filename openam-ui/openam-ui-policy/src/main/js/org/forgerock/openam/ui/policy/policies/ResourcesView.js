/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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

/*global window, define, $, _*/

define("org/forgerock/openam/ui/policy/policies/ResourcesView", [
    "org/forgerock/commons/ui/common/main/AbstractView" ,
    "org/forgerock/openam/ui/policy/common/StripedListView",
    "org/forgerock/openam/ui/policy/resources/CreatedResourcesView"
], function (AbstractView, StripedList, CreatedResourcesView) {
    var ResourcesView = AbstractView.extend({
        element: "#editResources",
        template: "templates/policy/policies/ResourcesStepTemplate.html",
        noBaseTemplate: true,

        render: function (data, callback) {
            _.extend(this.data, data);

            this.parentRender(function () {
                var promises = [], resolve = function () { return (promises[promises.length] = $.Deferred()).resolve; };

                this.availablePatternsView = new StripedList();
                this.availablePatternsView.render({
                    entity: this.data.entity,
                    title: $.t('policy.common.availablePatterns'),
                    items: this.data.options.availablePatterns,
                    clickItem: this.addPattern.bind(this)
                }, '#patterns', resolve());

                CreatedResourcesView.render(this.data, resolve());

                $.when.apply($, promises).done(function () {
                    if (callback) { callback(); }
                });
            });
        },

        addPattern: function (item) {
            this.data.options.newPattern = item;
            CreatedResourcesView.render(this.data);
        }
    });

    return new ResourcesView();
});