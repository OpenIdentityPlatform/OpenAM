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
 * Copyright 2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/user/anonymousProcess/SelfRegistrationView", [
    "lodash",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView"
], function (_, Constants, AnonymousProcessView, SelfRegistrationView) {

    function AMSelfRegistrationView () { }

    AMSelfRegistrationView.prototype = SelfRegistrationView;
    AMSelfRegistrationView.prototype.endpoint = Constants.SELF_SERVICE_REGISTER;

    _.extend(AMSelfRegistrationView.prototype, AnonymousProcessView.prototype);

    return new AMSelfRegistrationView();
});
