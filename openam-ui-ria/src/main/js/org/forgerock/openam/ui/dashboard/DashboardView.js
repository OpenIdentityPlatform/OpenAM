/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _ */

/**
 * @author mbilski
 */
define("org/forgerock/openam/ui/dashboard/DashboardView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/dashboard/DashboardDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, DashboardDelegate, eventManager, constants, conf) {
    
    var Dashboard = AbstractView.extend({
        template: "templates/openam/DashboardTemplate.html",
        render: function() {
            
            DashboardDelegate.getMyApplications(_.bind(function (apps) {
                this.data = {};
                this.data.apps = apps;
                this.parentRender(_.bind(function () {
                    if (this.data.apps.length === 0) {
                        this.$el.find("#appsList").text($.t("openam.apps.noneFound"));
                    }
                }, this));
                
            }, this));
            
        }
    });
    
    return new Dashboard();
});


