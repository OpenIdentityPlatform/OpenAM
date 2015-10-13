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


define("org/forgerock/openam/ui/uma/views/share/BaseShare", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/uma/views/share/CommonShare"
], function (AbstractView, CommonShare) {

    var BaseShare = AbstractView.extend({
        template: "templates/uma/views/share/BaseShare.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        render: function (args, callback) {

            var self = this;
            self.shareView = new CommonShare();
            self.shareView.element = "#commonShare";
            self.shareView.noBaseTemplate = true;
            self.parentRender(function () {
                self.data.resourceSet = {};
                self.shareView.render(args, callback);
            }, callback);
        }

    });

    return new BaseShare();
});
