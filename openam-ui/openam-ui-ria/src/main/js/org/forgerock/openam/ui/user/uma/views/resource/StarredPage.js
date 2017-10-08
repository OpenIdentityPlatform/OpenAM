/**
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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/openam/ui/user/uma/views/resource/BasePage",
    "org/forgerock/openam/ui/user/uma/services/UMAService"
], function (_, BasePage, UMAService) {
    var StarredPage = BasePage.extend({
        template: "templates/user/uma/views/resource/StarredPageTemplate.html",
        render (args, callback) {
            var self = this;

            UMAService.labels.all().done(function (data) {
                var starred = _.find(data.result, function (label) {
                    return label.name.toLowerCase() === "starred";
                });

                if (starred) {
                    self.renderGrid(self.createLabelCollection(starred._id), self.createColumns("starred"), callback);
                } else {
                    console.error("Unable to find \"starred\" label. " +
                                  "Label should have been created by UI on first load.");
                }
            });
        }
    });

    return StarredPage;
});
