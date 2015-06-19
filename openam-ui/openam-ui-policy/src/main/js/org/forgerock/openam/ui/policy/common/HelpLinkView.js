/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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

/*global window, define, $, _ */

define("org/forgerock/openam/ui/policy/common/HelpLinkView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (AbstractView, Configuration) {
    var HelpLinkView = AbstractView.extend({
        noBaseTemplate: true,
        template: 'templates/policy/common/HelpLinkTemplate.html',

        events: {
            'click .fa-question': 'openDocumentation',
            'keyup .fa-question': 'openDocumentation'
        },

        render: function ($el, customText, callback) {
            this.data.customText = customText ? customText : '';

            var documentation = Configuration.globalData.policyEditor && Configuration.globalData.policyEditor.documentation ?
                    Configuration.globalData.policyEditor.documentation : {},
                key = $el.data('help-key');

            this.element = $el;
            this.url = key ? key.split('.').reduce(function (prevVal, currVal) {
                if (prevVal) {
                    return prevVal[currVal];
                }
            }, documentation) : null;

            if (this.url || this.data.customText) {
                this.parentRender(function () {

                    this.$el.find('.fa[data-toggle="popover"]').popover();

                    if (callback) {
                        callback();
                    }
                });
            }
        },

        openDocumentation: function (e) {
            if (!this.url) {
                return;
            }

            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            window.open(this.url, '_blank');
        }
    });

    return HelpLinkView;
});