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

define("org/forgerock/openam/ui/uma/views/share/ShareCounter", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function ($, AbstractView) {
    var ShareCounter = AbstractView.extend({

        template: "templates/uma/views/share/ShareCounter.html",
        element: "#shareCounter",

        render: function (count, callback) {

            this.data.shareCount = count;
            this.data.shareInfo = this.getShareInfo(count);
            this.data.shareIcon = this.getShareIcon(count);

            this.parentRender(function () {
                if (callback) { callback(); }
            });
        },


        getShareInfo: function (count) {
            var options = count ? { count: count } : { context: "none" };
            return $.t("uma.share.info", options);
        },

        getShareIcon: function (count) {
            var shareIcon = "fa fa-lock";
            if (count === 1) {
                shareIcon = "fa fa-user";
            } else if (count > 1) {
                shareIcon = "fa fa-users";
            }
            return shareIcon;
        }
    });

    return new ShareCounter();
});
