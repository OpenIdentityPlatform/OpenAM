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

/*global $, _, define*/
define("org/forgerock/openam/ui/uma/views/share/FooterShare", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants"
], function(AbstractView, Constants) {
    var FooterShare = AbstractView.extend({

        template: "templates/uma/views/share/FooterShare.html",
        element: "#footerShare",

        render: function(count, callback) {

            this.data.shareCount = count;
            this.data.shareInfo = this.getShareInfo(count);
            this.data.shareIcon = this.getShareIcon(count);

            this.parentRender(function() {
                if(callback){callback();}
            });
        },


        getShareInfo: function(count) {
            var shareInfo = $.t("uma.share.info", { context : "none" } );
            if (count){
                shareInfo =  $.t("uma.share.info", { count: count });
            }
            return shareInfo;
        },

        getShareIcon: function(count) {
            var shareIcon = 'lock';
            if (count === 1){
                shareIcon = 'user';
            } else if (count > 1){
                shareIcon = 'users';
            }
            return shareIcon;
        }
    });

    return new FooterShare();
});
