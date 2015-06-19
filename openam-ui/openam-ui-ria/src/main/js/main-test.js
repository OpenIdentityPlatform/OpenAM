/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2015 ForgeRock AS. All rights reserved.
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except ins
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

/*global require, QUnit */

require.config({
    paths: {
        handlebars: "libs/handlebars-1.3.0-min",
        i18next: "libs/i18next-1.7.3-min",
        jquery: 'libs/jquery-2.1.1-min',
        moment: "libs/moment-2.8.1-min",
        squire: '../test-classes/libs/squire-0.2.0',
        underscore: "libs/lodash-2.4.1-min",
        xdate: "libs/xdate-0.8-min",
        'config/AppConfiguration': "../test-classes/config/AppConfiguration"
  }
});

require([
  '../test-classes/org/forgerock/openam/ui/common/util/RealmHelperTest'
], function(RealmHelper) {
  QUnit.start();
});
