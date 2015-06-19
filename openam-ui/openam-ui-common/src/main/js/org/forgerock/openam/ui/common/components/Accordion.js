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

define("org/forgerock/openam/ui/common/components/Accordion", function () {
    var Accordion;

    Accordion = function (parent, id) {
        var panel,
            currentCollapsePanel,
            nextBtn,
            nextCollapsePanel;

        this.$el = parent.$el.find(id);
        this.panels = this.$el.find('> .panel > .panel-collapse');

        _.each(this.$el.find('.panel-title > a'), function (link) {

            panel = $(link).parents('.panel');
            currentCollapsePanel = panel.find('> .panel-collapse');
            nextCollapsePanel = panel.next().find('> .panel-collapse');

            nextBtn = currentCollapsePanel.find('[name=nextButton]');

            currentCollapsePanel.collapse({
                toggle: false,
                parent: id
            });

            $(link).on('click', (function (panel) {
                return function (e) {
                    if (panel.hasClass('in')) {
                        e.stopPropagation();
                        e.preventDefault();
                        return;
                    }

                    panel.collapse('show');
                };
            }(currentCollapsePanel)));

            nextBtn.on('click', (function (currentPanel, nextPanel) {
                return function () {
                    currentPanel.on('hidden.bs.collapse', function () {
                        nextPanel.collapse('show');
                    });

                    currentPanel.collapse('hide');
                };
            }(currentCollapsePanel, nextCollapsePanel)));
        });

        if (parent.data.entity.name) {
            this.activeStep = this.panels.length - 1;
        }

        this.show(this.activeStep ? this.activeStep : 0);
    };


    Accordion.prototype.on = function (eventName, handler) {
        this.$el.on(eventName, handler);
    };

    Accordion.prototype.show = function (id) {
        $(this.panels[id]).collapse('show');
    };

    return Accordion;
});