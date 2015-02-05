/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

require(['squire'], function(Squire) {
    new Squire()
    .store('org/forgerock/commons/ui/common/util/UIUtils')
    .require([
        'org/forgerock/openam/ui/common/util/RealmHelper',
        'mocks'
    ], function(subject, mocks) {
        module('org/forgerock/openam/ui/common/util/RealmHelper', {
          beforeEach: function() {
            // TODO: Don't drop uiUtils into the global namespace
            uiUtils = mocks.store['org/forgerock/commons/ui/common/util/UIUtils'];
          }
        });
        test('#getRealm when realm is not present', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login');

            equal(subject.getRealm(), '/', 'returns root realm');
        }));

        test('#getRealm when realm is present in fragment', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login/realmA');

            equal(subject.getRealm(), '/realmA', 'returns realm from fragment');
        }));

        test('#getRealm when realm is present in URI query string', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login/');
            this.stub(uiUtils, 'getURIQueryString').returns('realm=realmA');

            equal(subject.getRealm(), '/realmA', 'returns realm from URI query string');
        }));

        test('#getRealm when realm is present in fragment query string', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login/&realm=realmA');

            equal(subject.getRealm(), '/realmA', 'returns realm from fragment query string');
        }));

        test('#getRealm when realm is inconsistent', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login/realmB&realm=realmC');
            this.stub(uiUtils, 'getURIQueryString').returns('realm=realmA');

            equal(subject.getRealm(), null, 'returns null');
        }));

        test('#getRealm when realm is inconsistent and fragment is not present', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login/&realm=realmC');
            this.stub(uiUtils, 'getURIQueryString').returns('realm=realmA');

            equal(subject.getRealm(), null, 'returns null');
        }));

        test('#getRealm when realm is inconsistent and URI query string is not present', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login/realmB&realm=realmC');

            equal(subject.getRealm(), null, 'returns null');
        }));

        test('#getRealm when realm is inconsistent and fragment query string is not present', sinon.test(function() {
            this.stub(uiUtils, 'getURIFragment').returns('login/realmB');
            this.stub(uiUtils, 'getURIQueryString').returns('realm=realmA');

            equal(subject.getRealm(), null, 'returns null');
        }));
    });
});
